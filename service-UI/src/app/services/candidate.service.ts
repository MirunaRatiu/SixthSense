import { Injectable } from "@angular/core"
import {  HttpClient, HttpParams,HttpHeaders } from "@angular/common/http"
import {Observable, catchError, throwError, of, BehaviorSubject, map} from "rxjs"
import { environment } from "../../environments/environment"
import  { Candidate } from "../models/candidate.model"

interface ViewerUrlResponse {
  originalUrl: string;
  viewerUrl: string;
  error?: string; // Adaugă proprietatea error opțională
}

@Injectable({
  providedIn: "root",
})
export class CandidateService {
  private apiUrl = `${environment.apiUrl}/cv`
  private viewApiUrl = `${environment.apiUrl}/view`
  private selectedCandidateSubject = new BehaviorSubject<Candidate | null>(null)
  selectedCandidate$ = this.selectedCandidateSubject.asObservable()

  constructor(private http: HttpClient) {}

  getCandidates(filters?: any): Observable<Candidate[]> {
    let params = new HttpParams()

    if (filters) {
      Object.keys(filters).forEach((key) => {
        if (filters[key]) {
          params = params.append(key, filters[key])
        }
      })
    }

    return this.http.get<Candidate[]>(`${this.apiUrl}/all`, { params }).pipe(catchError(this.handleError))
  }

  getCandidateById(id: number): Observable<Candidate> {
    return this.http.get<Candidate>(`${this.apiUrl}/${id}`).pipe(catchError(this.handleError))
  }

  createCandidate(candidate: Partial<Candidate>): Observable<Candidate> {
    return this.http.post<Candidate>(this.apiUrl, candidate).pipe(catchError(this.handleError))
  }

  updateCandidate(candidate: Candidate): Observable<Candidate> {
    return this.http.put<Candidate>(`${this.apiUrl}/${candidate.id}`, candidate).pipe(catchError(this.handleError))
  }

  deleteCandidate(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(catchError(this.handleError))
  }

  // Delete multiple candidates in a single request
  deleteCandidates(ids: number[]): Observable<any> {
    const options = {
      headers: new HttpHeaders({
        "Content-Type": "application/json",
      }),
      body: ids, // This will be sent as the request body
    }

    // Using DELETE method with a request body
    return this.http.delete(`${this.apiUrl}/delete`, options).pipe(
      catchError((error) => {
        console.error(`Error deleting candidates:`, error)
        return throwError(() => new Error("Failed to delete candidates. Please try again."))
      }),
    )
  }
  setSelectedCandidate(candidate: Candidate): void {
    this.selectedCandidateSubject.next(candidate)
  }

  getSelectedCandidate(): Candidate | null {
    return this.selectedCandidateSubject.value
  }

  private handleError(error: any) {
    console.error("An error occurred:", error)
    return throwError(() => new Error("Something went wrong. Please try again later."))
  }

  getViewerUrl(originalDocUrl: string): Observable<string> {
    if (!originalDocUrl) {
      return throwError(() => new Error("Original document URL is required."));
    }

    // Encodează URL-ul original pentru a fi trimis în query string
    const encodedDocUrl = encodeURIComponent(originalDocUrl);
    const url = `${this.viewApiUrl}/generate-viewer-url?docUrl=${encodedDocUrl}`;

    console.log('Requesting viewer URL:', url); // Debug log

    return this.http.get<ViewerUrlResponse>(url).pipe(
      map(response => {
        if (!response || !response.viewerUrl) {
          throw new Error(response?.error || 'Backend did not return a valid viewer URL.');
        }
        return response.viewerUrl; // Returnează doar URL-ul de vizualizare
      }),
      catchError(this.handleError) // Refolosește handler-ul general de erori
    );
  }
}
