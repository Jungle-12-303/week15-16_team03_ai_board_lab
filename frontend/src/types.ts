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
