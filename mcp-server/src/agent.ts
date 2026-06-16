import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

type WeatherDraft = {
  title: string;
  content: string;
  tags: string[];
  sourceSummary: string[];
};

type AgentArgs = {
  city: string;
  forecastDays: number;
  jsonMode: boolean;
};

function readArgs(): AgentArgs {
  const city = process.argv[2] ?? "Seoul";
  const rawForecastDays = Number(process.argv[3] ?? "2");
  const jsonMode = process.argv.includes("--json");

  if (Number.isNaN(rawForecastDays) || rawForecastDays < 1 || rawForecastDays > 3) {
    throw new Error("forecastDays는 1~3 사이의 숫자여야 합니다.");
  }

  return {
    city,
    forecastDays: rawForecastDays,
    jsonMode,
  };
}

function readTextContent(result: unknown): string {
  if (typeof result !== "object" || result === null || !("content" in result)) {
    throw new Error("MCP 도구 결과 형식이 예상과 다릅니다.");
  }

  const toolResult = result as {
    content?: Array<{ type: string; text?: string }>;
  };

  const textItem = toolResult.content?.find((item) => item.type === "text");

  if (textItem?.text === undefined) {
    throw new Error("MCP 도구 결과에서 text 응답을 찾지 못했습니다.");
  }

  return textItem.text;
}

function parseDraftText(text: string): WeatherDraft {
  try {
    return JSON.parse(text) as WeatherDraft;
  } catch {
    throw new Error(`MCP 도구가 JSON 대신 다음 값을 반환했습니다: ${text}`);
  }
}

function printDraft(draft: WeatherDraft) {
  console.log("=== MCP Agent Result ===");
  console.log("");
  console.log("[추천 제목]");
  console.log(draft.title);
  console.log("");
  console.log("[추천 태그]");
  console.log(draft.tags.join(", "));
  console.log("");
  console.log("[게시글 본문 초안]");
  console.log(draft.content);
  console.log("");
  console.log("[참고 데이터 요약]");

  for (const line of draft.sourceSummary) {
    console.log(`- ${line}`);
  }
}

async function main() {
  const { city, forecastDays, jsonMode } = readArgs();

  const client = new Client({
    name: "ai-board-weather-agent",
    version: "0.1.0",
  });

  const transport = new StdioClientTransport({
    command: process.execPath,
    args: ["dist/index.js"],
    cwd: process.cwd(),
    stderr: "inherit",
  });

  client.onerror = (error) => {
    console.error("MCP client error:", error);
  };

  await client.connect(transport);

  if (!jsonMode) {
    const tools = await client.listTools();
    console.log("사용 가능한 MCP 도구:", tools.tools.map((tool) => tool.name).join(", "));
    console.log("선택한 도구: create_weather_briefing_draft");
    console.log(`입력 도시: ${city}, 예보 일수: ${forecastDays}`);
    console.log("");
  }

  const result = await client.callTool({
    name: "create_weather_briefing_draft",
    arguments: {
      city,
      forecastDays,
    },
  });

  const text = readTextContent(result);
  const draft = parseDraftText(text);

  if (jsonMode) {
    console.log(JSON.stringify(draft));
  } else {
    printDraft(draft);
  }

  await transport.close();
}

main().catch((error) => {
  console.error("MCP agent failed:", error);
  process.exit(1);
});
