export interface Candidate {
  id: number
  name: string
  skills: string[]
  languages: string[]
  education: string
  certifications: string[]
  project_experience: string[]
  work_experience: string[]
  others: string[]
  matchScore: number
  score: number
  accessLink?: string // Add the accessLink property
 // Any other fields that might be in the backend response
 [key: string]: any

}
