export type Tag = {
  id: number
  name: string
}

export type Post = {
  id: number
  title: string
  content: string
  authorName: string
  createdAt: string
  tags: Tag[]
  ownerLoginId: string
}

export type Comment = {
  id: number
  content: string
  authorName: string
  createdAt: string
  ownerLoginId: string
}

export type RagReference = {
  postId: number
  title: string
  authorName: string
  createdAt: string
  contentPreview: string
  matchedChunkText: string
  similarityScore: number
  tags: Tag[]
}

export type RagAnswerResponse = {
  question: string
  answer: string
  answerModel: string
  embeddingModel: string
  totalMatches: number
  references: RagReference[]
}

export type RagReindexResponse = {
  requestedBy: string
  embeddingModel: string
  totalPosts: number
  successCount: number
  failedCount: number
  failedPostIds: number[]
}

export type RagStatusResponse = {
  apiKeyConfigured: boolean
  embeddingModel: string
  chatModel: string
  embeddingDimensions: number
  indexedPostCount: number
  embeddingRowCount: number
}

export type McpWeatherDraftResponse = {
  requestedBy: string
  city: string
  forecastDays: number
  title: string
  content: string
  tags: string[]
  sourceSummary: string[]
}
