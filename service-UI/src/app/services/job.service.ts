import { Injectable } from "@angular/core"
import { HttpClient } from "@angular/common/http"
import {  Observable, of, BehaviorSubject } from "rxjs"
import  { Job } from "../models/job.model"

@Injectable({
  providedIn: "root",
})
export class JobService {
  // Mock data for demonstration
  private mockJobs: Job[] = [
    {
      id: 1,
      positionName: "3D Artist",
      client: "Apple",
      clientLogo: "assets/logos/apple.png",
      location: "Seattle, WA, United States",
      headcount: "26 - 1",
      stage: "1ST INTERVIEW",
      salary: null,
      requiredSkills: ["3D Modeling", "Blender", "Maya", "Texturing"],
      description: "Create 3D models and assets for Apple products and marketing materials.",
      experience: 3,
    },
    {
      id: 2,
      positionName: "Animator",
      client: "Amazon",
      clientLogo: "assets/logos/amazon.png",
      location: "New York, NY, United States",
      headcount: "14 - 1",
      stage: "HIRED",
      salary: null,
      requiredSkills: ["2D Animation", "3D Animation", "After Effects", "Character Animation"],
      description: "Create animations for Amazon marketing campaigns and product demonstrations.",
      experience: 2,
    },
    {
      id: 3,
      positionName: "Business Manager",
      client: "Amazon",
      clientLogo: "assets/logos/amazon.png",
      location: "Boston, MA, United States",
      headcount: "4 - 1",
      stage: "PROBATION PASS",
      salary: null,
      requiredSkills: ["Business Development", "Team Management", "Strategic Planning", "Budget Management"],
      description: "Manage business operations for Amazon retail division.",
      experience: 5,
    },
    {
      id: 4,
      positionName: "Chief Executive Officer (CEO)",
      client: "Instagram",
      clientLogo: "assets/logos/instagram.png",
      location: "San Francisco, CA, United States",
      headcount: "10 - 1",
      stage: "PROBATION PASS",
      salary: null,
      requiredSkills: ["Leadership", "Strategic Planning", "Business Development", "Executive Management"],
      description: "Lead the company and make high-level strategic decisions.",
      experience: 10,
    },
    {
      id: 5,
      positionName: "Chief Executive Officer (CEO)",
      client: "Hubspot",
      clientLogo: "assets/logos/hubspot.png",
      location: "New York, NY, United States",
      headcount: "20 - 1",
      stage: "PROBATION PASS",
      salary: null,
      requiredSkills: ["Leadership", "Strategic Planning", "Business Development", "Executive Management"],
      description: "Lead the company and make high-level strategic decisions.",
      experience: 10,
    },
    {
      id: 6,
      positionName: "Chief Marketing Officer (CMO)",
      client: "Spotify",
      clientLogo: "assets/logos/spotify.png",
      location: "Stockton, CA, United States",
      headcount: "3 - 1",
      stage: "PROBATION PASS",
      salary: null,
      requiredSkills: ["Marketing Strategy", "Brand Management", "Digital Marketing", "Team Leadership"],
      description: "Lead marketing efforts and develop marketing strategies.",
      experience: 8,
    },
    {
      id: 7,
      positionName: "Chief Operating Officer (COO)",
      client: "Instagram",
      clientLogo: "assets/logos/instagram.png",
      location: "San Francisco, CA, United States",
      headcount: "6 - 1",
      stage: "HIRED",
      salary: null,
      requiredSkills: ["Operations Management", "Strategic Planning", "Team Leadership", "Process Improvement"],
      description: "Oversee day-to-day operations and implement business strategies.",
      experience: 8,
    },
    {
      id: 8,
      positionName: "Chief Operating Officer (COO)",
      client: "Amazon",
      clientLogo: "assets/logos/amazon.png",
      location: "Dallas, TX, United States",
      headcount: "17 - 1",
      stage: "HR INTERVIEW",
      salary: null,
      requiredSkills: ["Operations Management", "Strategic Planning", "Team Leadership", "Process Improvement"],
      description: "Oversee day-to-day operations and implement business strategies.",
      experience: 8,
    },
    {
      id: 9,
      positionName: "Chief Product Officer",
      client: "Instagram",
      clientLogo: "assets/logos/instagram.png",
      location: "San Francisco, CA, United States",
      headcount: "3 - 2",
      stage: "HIRED",
      salary: "45,000 USD",
      requiredSkills: ["Product Management", "Product Strategy", "UX/UI Design", "Agile Methodologies"],
      description: "Lead product development and strategy.",
      experience: 7,
    },
    {
      id: 10,
      positionName: "Computer System Analyst",
      client: "Instagram",
      clientLogo: "assets/logos/instagram.png",
      location: "Palo Alto, CA, United States",
      headcount: "0 - 2",
      stage: "",
      salary: null,
      requiredSkills: ["System Analysis", "IT Infrastructure", "Problem Solving", "Technical Documentation"],
      description: "Analyze computer systems and recommend improvements.",
      experience: 4,
    },
  ]

  // constructor removed as HttpClient is not used
  private selectedJobSubject = new BehaviorSubject<Job | null>(null)
  selectedJob$ = this.selectedJobSubject.asObservable()

  constructor(private http: HttpClient) {}

  getJobs(): Observable<Job[]> {
    // In a real application, this would be an HTTP request to your backend
    return of(this.mockJobs)
  }

  getJobById(id: number): Observable<Job | undefined> {
    return of(this.mockJobs.find((job) => job.id === id))
  }

  setSelectedJob(job: Job) {
    this.selectedJobSubject.next(job)
  }

  getSelectedJob(): Job | null {
    return this.selectedJobSubject.value
  }
}
