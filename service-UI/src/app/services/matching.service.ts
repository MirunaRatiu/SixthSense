import { Injectable } from "@angular/core"
import { HttpClient } from "@angular/common/http"
import { type Observable, of } from "rxjs"
import type { Job } from "../models/job.model"
import type { Candidate } from "../models/candidate.model"

@Injectable({
  providedIn: "root",
})
export class MatchingService {
  // Mock data for demonstration
  private mockCandidates: Candidate[] = [
    {
      id: 1,
      name: "John Smith",
      avatar: "assets/avatars/avatar1.png",
      skills: ["JavaScript", "React", "Node.js", "TypeScript", "MongoDB"],
      experience: 5,
      education: "Bachelor of Computer Science",
      matchScore: 95,
    },
    {
      id: 2,
      name: "Emily Johnson",
      avatar: "assets/avatars/avatar2.png",
      skills: ["Python", "Django", "SQL", "Data Analysis", "Machine Learning"],
      experience: 3,
      education: "Master of Data Science",
      matchScore: 88,
    },
    {
      id: 3,
      name: "Michael Brown",
      avatar: "assets/avatars/avatar3.png",
      skills: ["Java", "Spring Boot", "Microservices", "Docker", "Kubernetes"],
      experience: 7,
      education: "Bachelor of Software Engineering",
      matchScore: 82,
    },
    {
      id: 4,
      name: "Sarah Davis",
      avatar: "assets/avatars/avatar4.png",
      skills: ["UX/UI Design", "Figma", "Adobe XD", "Sketch", "User Research"],
      experience: 4,
      education: "Bachelor of Design",
      matchScore: 78,
    },
    {
      id: 5,
      name: "David Wilson",
      avatar: "assets/avatars/avatar5.png",
      skills: ["C#", ".NET", "Azure", "SQL Server", "RESTful APIs"],
      experience: 6,
      education: "Master of Computer Science",
      matchScore: 75,
    },
  ]

  private mockJobs: Job[] = [
    {
      id: 1,
      positionName: "Frontend Developer",
      client: "Tech Solutions Inc.",
      clientLogo: "assets/logos/techsolutions.png",
      location: "Remote",
      headcount: "2 - 1",
      stage: "OPEN",
      salary: "80,000 USD",
      requiredSkills: ["JavaScript", "React", "TypeScript", "HTML", "CSS"],
      description: "We are looking for a Frontend Developer to join our team.",
      experience: 3,
      matchScore: 92,
    },
    {
      id: 2,
      positionName: "Backend Developer",
      client: "Data Systems",
      clientLogo: "assets/logos/datasystems.png",
      location: "New York, NY",
      headcount: "1 - 1",
      stage: "OPEN",
      salary: "90,000 USD",
      requiredSkills: ["Node.js", "Express", "MongoDB", "RESTful APIs", "GraphQL"],
      description: "Backend Developer position for our growing team.",
      experience: 4,
      matchScore: 85,
    },
    {
      id: 3,
      positionName: "Full Stack Developer",
      client: "Web Innovators",
      clientLogo: "assets/logos/webinnovators.png",
      location: "San Francisco, CA",
      headcount: "3 - 1",
      stage: "OPEN",
      salary: "110,000 USD",
      requiredSkills: ["JavaScript", "React", "Node.js", "MongoDB", "AWS"],
      description: "Looking for a Full Stack Developer to work on our main product.",
      experience: 5,
      matchScore: 78,
    },
  ]

  constructor() {}

  findMatchingCandidates(jobDescription: any): Observable<Candidate[]> {
    // In a real application, this would be an HTTP request to your backend
    // which would perform the actual matching algorithm

    // For demonstration, we'll return filtered mock data based on the job
    // In a real application, the backend would analyze the job description
    // and find candidates with matching skills

    if (!jobDescription) {
      return of([])
    }

    // Filter candidates based on job skills
    let filteredCandidates = [...this.mockCandidates]

    // If the job is a 3D Artist, return candidates with 3D skills
    if (jobDescription.positionName.includes("3D Artist")) {
      filteredCandidates = filteredCandidates.filter((candidate) =>
        candidate.skills.some((skill) => skill.includes("3D") || skill.includes("Blender") || skill.includes("Maya")),
      )
    }
    // If the job is an Animator, return candidates with animation skills
    else if (jobDescription.positionName.includes("Animator")) {
      filteredCandidates = filteredCandidates.filter((candidate) =>
        candidate.skills.some((skill) => skill.includes("Animation") || skill.includes("After Effects")),
      )
    }
    // If the job is a Business Manager or C-level position, return candidates with business skills
    else if (
      jobDescription.positionName.includes("Manager") ||
      jobDescription.positionName.includes("Chief") ||
      jobDescription.positionName.includes("Officer")
    ) {
      filteredCandidates = filteredCandidates.filter((candidate) =>
        candidate.skills.some(
          (skill) =>
            skill.includes("Business") ||
            skill.includes("Management") ||
            skill.includes("Leadership") ||
            skill.includes("Strategic"),
        ),
      )
    }
    // For other positions, return a mix of candidates
    else {
      // Just return all candidates with adjusted scores
      filteredCandidates = filteredCandidates.map((candidate) => ({
        ...candidate,
        matchScore: Math.floor(Math.random() * 30) + 70, // Random score between 70-99
      }))
    }

    // Sort by match score
    filteredCandidates.sort((a, b) => b.matchScore - a.matchScore)

    // Return top 5 candidates
    return of(filteredCandidates.slice(0, 5))
  }

  findMatchingJobs(candidateData: {
    name: string;
    skills: string;
    experience: number;
  }): Observable<Job[]> {
    // In a real application, this would be an HTTP request to your backend
    // which would perform the actual matching algorithm

    // For demonstration, we'll return mock data
    // In a real application, the backend would analyze the CV
    // and find jobs with matching requirements

    return of(this.mockJobs)
  }
}
