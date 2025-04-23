export interface Job {
  id: number
  positionName: string
  client: string
  clientLogo: string
  location: string
  headcount: string
  stage: string
  salary: string | null
  requiredSkills: string[]
  description: string
  experience: number
  matchScore?: number
}
