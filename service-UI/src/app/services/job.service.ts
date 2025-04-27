import { Injectable } from "@angular/core"
import {  HttpClient, HttpHeaders, HttpParams } from "@angular/common/http"
import {  Observable, catchError, throwError, BehaviorSubject, of } from "rxjs"
import { environment } from "../../environments/environment"
import  { Job } from "../models/job.model"

@Injectable({
  providedIn: "root",
})
export class JobService {
  private apiUrl = `${environment.apiUrl}/jobDescription`
  private selectedJobSubject = new BehaviorSubject<Job | null>(null)
  selectedJob$ = this.selectedJobSubject.asObservable()

  constructor(private http: HttpClient) {}

  getJobs(filters?: any): Observable<Job[]> {
    let params = new HttpParams()

    if (filters) {
      Object.keys(filters).forEach((key) => {
        if (filters[key]) {
          params = params.append(key, filters[key])
        }
      })
    }

    return this.http.get<Job[]>(`${this.apiUrl}/all`, { params }).pipe(
      catchError((error) => {
        console.error("Error fetching jobs:", error)
        return throwError(() => new Error("Something went wrong. Please try again later."))
      }),
    )
  }

  getJobById(id: number): Observable<Job> {
    return this.http.get<Job>(`${this.apiUrl}/${id}`).pipe(catchError(this.handleError))
  }

  createJob(job: Job): Observable<Job> {
    return this.http.post<Job>(this.apiUrl, job).pipe(catchError(this.handleError))
  }

  updateJob(job: Job): Observable<Job> {
    return this.http.put<Job>(`${this.apiUrl}/${job.id}`, job).pipe(catchError(this.handleError))
  }

  // Updated to accept an array of IDs
  deleteJob(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`).pipe(
      catchError((error) => {
        console.error(`Error deleting job ${id}:`, error)
        return of(null)
      }),
    )
  }

  // New method to delete multiple jobs at once
  deleteJobs(ids: number[]): Observable<any> {
    const options = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
      }),
      body: ids // This will be sent as the request body
    };
    
    // Using DELETE method with a request body
    return this.http.delete(`${this.apiUrl}/delete`, options).pipe(
      catchError((error) => {
        console.error(`Error deleting jobs:`, error)
        return of(null)
      }),
    )
  }
  setSelectedJob(job: Job): void {
    this.selectedJobSubject.next(job)
  }

  getSelectedJob(): Job | null {
    return this.selectedJobSubject.value
  }

  private handleError(error: any) {
    console.error("An error occurred:", error)
    return throwError(() => new Error("Something went wrong. Please try again later."))
  }
}
