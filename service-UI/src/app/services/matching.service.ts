import { Injectable } from "@angular/core"
import  { HttpClient } from "@angular/common/http"
import {  Observable, catchError, throwError } from "rxjs"
import { environment } from "../../environments/environment"
import  { Candidate } from "../models/candidate.model"
import  { Job } from "../models/job.model"

interface JobMatchRequestPayload {
  jd: Job;
  job_skills: Record<string, number>;
}

@Injectable({
  providedIn: "root",
})
export class MatchingService {
  private apiUrl = `${environment.apiUrl}/match`

  constructor(private http: HttpClient) {}

  findMatchingCandidates(job: Job, skillWeights?: any): Observable<Candidate[]> {
    return this.http
      .post<Candidate[]>(`${this.apiUrl}/cv/${job.id}`, {
        skillWeights: skillWeights || {},
      })
      .pipe(catchError(this.handleError))
  }

  findMatchingJobs(candidateData: any): Observable<Job[]> {
    return this.http.post<Job[]>(`${this.apiUrl}/jd`, candidateData).pipe(catchError(this.handleError))
  }

  private handleError(error: any) {
    console.error("An error occurred:", error)
    return throwError(() => new Error("Something went wrong. Please try again later."))
  }
}
