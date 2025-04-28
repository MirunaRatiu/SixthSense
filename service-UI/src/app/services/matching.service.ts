import { Injectable } from "@angular/core"
import {HttpClient, HttpHeaders, HttpParams} from "@angular/common/http"
import {  Observable, catchError, throwError } from "rxjs"
import { environment } from "../../environments/environment"
import  { Candidate } from "../models/candidate.model"
import  { Job } from "../models/job.model"
// import {JobMatchRequestPayload} from '../models/JobMatchRequestPayload.model';
// import {CvMatchResponseDTO} from '../models/CvMatchResponseDTO';

interface JobViewMatchPayload {
  jdId: number; // Matches backend private Integer jdId;
  additionalSkills: Record<string, number>; // Matches backend private Map<String, Integer> additionalSkills;
  // Add aspect weights here if the backend DTO was updated to include them
  // aspectWeights?: Record<string, number>; // Matches backend private Map<String, Integer> aspectWeights;
}

@Injectable({
  providedIn: "root",
})
export class MatchingService {
  private apiUrl = `${environment.apiUrl}/match`

  constructor(private http: HttpClient) {}

  // In MatchingService
  findMatchingCandidates(payload: JobViewMatchPayload): Observable<Candidate[]> {
    // Targetting the backend endpoint for matching candidates by JD
    // This endpoint MUST be configured on the backend to accept a @RequestBody JobViewMatchDTO
    const actualEndpointUrl = `${this.apiUrl}/jd`;

    console.log(`MatchingService: Sending POST request to: ${actualEndpointUrl}`);
    console.log('MatchingService: JSON Body Payload:', JSON.stringify(payload, null, 2)); // Log the object being sent

    // Send the 'payload' object directly as the request body for a POST request.
    // HttpClient automatically serializes the object to JSON and sets Content-Type to application/json.
    return this.http
      .post<Candidate[]>(actualEndpointUrl, payload) // <-- Send the payload object as the JSON body
      .pipe(catchError(this.handleError));
  }

  findMatchingJobs(candidateId: number): Observable<Job[]> {
    // Use HttpParams to add the cvId as a query parameter
    const params = new HttpParams().set("cvId", candidateId.toString()) // <-- Aici se creează parametrii

    // Make a GET request to the endpoint with the query parameter
    return this.http.get<Job[]>(`${this.apiUrl}/cv`, { params }).pipe( // <-- HttpClient.get folosește corect obiectul params
      catchError((error) => {
        console.error("Error finding matching jobs:", error)
        return throwError(() => new Error("Failed to find matching jobs. Please try again."))
      }),
    )
  }

  private handleError(error: any) {
    console.error("An error occurred:", error)
    return throwError(() => new Error("Something went wrong. Please try again later."))
  }
}
