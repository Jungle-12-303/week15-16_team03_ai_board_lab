import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

type GeocodeResult = {
  name: string;
  latitude: number;
  longitude: number;
  country?: string;
  admin1?: string;
};

type GeocodeResponse = {
  results?: GeocodeResult[];
};

type ForecastResponse = {
  timezone?: string;
  current?: {
    temperature_2m?: number;
    weather_code?: number;
    wind_speed_10m?: number;
  };
  daily?: {
    time?: string[];
    weather_code?: number[];
    temperature_2m_max?: number[];
    temperature_2m_min?: number[];
    precipitation_probability_max?: number[];
  };
};

type WeatherDraft = {
  title: string;
  content: string;
  tags: string[];
  sourceSummary: string[];
};

const server = new McpServer({
  name: "ai-board-weather-mcp",
  version: "0.1.0",
});

function getWeatherLabel(weatherCode?: number): string {
  switch (weatherCode) {
    case 0:
      return "맑음";
    case 1:
    case 2:
    case 3:
      return "구름 많음";
    case 45:
    case 48:
      return "안개";
    case 51:
    case 53:
    case 55:
    case 56:
    case 57:
      return "이슬비";
    case 61:
    case 63:
    case 65:
    case 66:
    case 67:
      return "비";
    case 71:
    case 73:
    case 75:
    case 77:
      return "눈";
    case 80:
    case 81:
    case 82:
      return "소나기";
    case 85:
    case 86:
      return "소낙눈";
    case 95:
    case 96:
    case 99:
      return "뇌우";
    default:
      return "기타 날씨";
  }
}

async function fetchJson<T>(url: string): Promise<T> {
  let response: Response;

  try {
    response = await fetch(url);
  } catch (error) {
    const message = error instanceof Error ? error.message : "알 수 없는 네트워크 오류";
    throw new Error(`외부 날씨 API에 연결하지 못했습니다: ${message}`);
  }

  if (!response.ok) {
    throw new Error(`외부 API 호출 실패: ${response.status}`);
  }

  return (await response.json()) as T;
}

async function findLocation(city: string): Promise<GeocodeResult> {
  const encodedCity = encodeURIComponent(city);
  const url = `https://geocoding-api.open-meteo.com/v1/search?name=${encodedCity}&count=1&language=ko&format=json`;
  const data = await fetchJson<GeocodeResponse>(url);

  if (data.results === undefined || data.results.length === 0) {
    throw new Error("입력한 도시를 찾을 수 없습니다.");
  }

  return data.results[0];
}

async function getForecast(location: GeocodeResult, forecastDays: number): Promise<ForecastResponse> {
  const url =
    `https://api.open-meteo.com/v1/forecast` +
    `?latitude=${location.latitude}` +
    `&longitude=${location.longitude}` +
    `&current=temperature_2m,weather_code,wind_speed_10m` +
    `&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max` +
    `&timezone=auto` +
    `&forecast_days=${forecastDays}`;

  return fetchJson<ForecastResponse>(url);
}

function buildDailySummary(forecast: ForecastResponse): string[] {
  const days = forecast.daily;

  if (
    days?.time === undefined ||
    days.weather_code === undefined ||
    days.temperature_2m_max === undefined ||
    days.temperature_2m_min === undefined ||
    days.precipitation_probability_max === undefined
  ) {
    return [];
  }

  return days.time.map((date, index) => {
    const label = getWeatherLabel(days.weather_code?.[index]);
    const max = days.temperature_2m_max?.[index];
    const min = days.temperature_2m_min?.[index];
    const rainChance = days.precipitation_probability_max?.[index];

    return `${date}: ${label}, 최고 ${max}도 / 최저 ${min}도, 강수확률 ${rainChance}%`;
  });
}

function buildDraft(city: string, location: GeocodeResult, forecast: ForecastResponse): WeatherDraft {
  const currentTemp = forecast.current?.temperature_2m;
  const currentWeather = getWeatherLabel(forecast.current?.weather_code);
  const currentWind = forecast.current?.wind_speed_10m;
  const dailySummary = buildDailySummary(forecast);

  const areaLabel = [location.admin1, location.country].filter(Boolean).join(", ");
  const placeLabel = areaLabel === "" ? location.name : `${location.name} (${areaLabel})`;

  const title = `${location.name} 날씨 브리핑 - 현재 ${currentWeather}`;

  const contentLines = [
    `${placeLabel} 기준 날씨 브리핑입니다.`,
    ``,
    `현재 기온은 ${currentTemp}도이고, 현재 날씨 상태는 ${currentWeather}입니다. 풍속은 ${currentWind}km/h 수준입니다.`,
    ``,
    `예보 요약`,
    ...dailySummary.map((line) => `- ${line}`),
    ``,
    `운영 메모`,
    `- 이 초안은 Open-Meteo 공개 날씨 데이터를 바탕으로 생성했습니다.`,
    `- 실제 게시판에 등록할 때는 지역 행사, 교통, 우산 필요 여부 같은 문장을 추가하면 더 자연스럽습니다.`,
  ];

  const tags = ["날씨", location.name, currentWeather];

  return {
    title,
    content: contentLines.join("\n"),
    tags,
    sourceSummary: [
      `도시: ${placeLabel}`,
      `현재 기온: ${currentTemp}도`,
      `현재 날씨: ${currentWeather}`,
      `현재 풍속: ${currentWind}km/h`,
      ...dailySummary,
    ],
  };
}

server.tool(
  "create_weather_briefing_draft",
  {
    city: z.string().min(1, "도시 이름은 비어 있을 수 없습니다."),
    forecastDays: z.number().int().min(1).max(3).optional(),
  },
  async ({ city, forecastDays }) => {
    const normalizedForecastDays = forecastDays ?? 2;
    const location = await findLocation(city);
    const forecast = await getForecast(location, normalizedForecastDays);
    const draft = buildDraft(city, location, forecast);

    return {
      content: [
        {
          type: "text",
          text: JSON.stringify(draft, null, 2),
        },
      ],
    };
  },
);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("AI board MCP server started.");
}

main().catch((error) => {
  console.error("MCP server failed to start:", error);
  process.exit(1);
});
