import { Injectable } from "@angular/core"
import {  HttpClient,  HttpEvent, HttpRequest, HttpErrorResponse } from "@angular/common/http"
import {  Observable, catchError, throwError } from "rxjs"
import { environment } from "../../environments/environment"

@Injectable({
  providedIn: "root",
})
export class UploadService {
   private apiUrl = `${environment.apiUrl}/files/upload-jobDescription`
    private apiUrl2= `${environment.apiUrl}/files/upload-cv`
  constructor(private http: HttpClient) {}

  uploadCV(file: File): Observable<HttpEvent<any>> {
    const formData: FormData = new FormData()
    formData.append("file", file)

    return this.http.post(`${this.apiUrl2}`, formData, {
      reportProgress: true,
      observe: 'events',
    }).pipe(
      catchError(this.handleError)
    );

  
  }

  parseCV(fileId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/cv/${fileId}/parse`).pipe(catchError(this.handleError))
  }


    /**
   * Upload a job description file to the server
   * @param file The file to upload
   * @returns Observable of the upload progress and response
   */
    uploadJobDescription(file: File): Observable<HttpEvent<any>> {
      const formData: FormData = new FormData();
      formData.append("file", file);
    
      return this.http.post(`${this.apiUrl}`, formData, {
        reportProgress: true,
        observe: 'events',
      }).pipe(
        catchError(this.handleError)
      );
    }

    
    private handleError(error: HttpErrorResponse) {
      let errorMessage = "An unknown error occurred during the upload"
  
      if (error.error instanceof ErrorEvent) {
        // Client-side error
        errorMessage = `Error: ${error.error.message}`
      } else {
        // Server-side error
        errorMessage = `The automatic processing was not possible due to the scanned text or images in the CV. Please upload a CV in .docx format.`
      }
  
      console.error(errorMessage)
      return throwError(() => new Error(errorMessage))
    }
}
