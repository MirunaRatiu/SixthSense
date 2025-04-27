export interface Job {
  id: number
  jobTitle?: string
  companyOverview?: string
  requiredQualifications?: string
  preferredSkills?: string[]
  benefits?: string[]
  message?: string
  createdAt?: Date
  keyResponsabilities?: string
  // Any other fields that might be in the backend response
  [key: string]: any
  accessLink?: string // Add the accessLink property
}


/*
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
*/