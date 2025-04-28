import {CvViewDTO} from './CvViewDTO';
import {Candidate} from './candidate.model';

export interface CvMatchResponseDTO {
  score: number;
  explanation: Record<string, string>;
  cvViewDTO: CvViewDTO | null;
}

