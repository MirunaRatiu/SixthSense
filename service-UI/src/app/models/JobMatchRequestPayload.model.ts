import {Job} from './job.model';

export interface JobMatchRequestPayload {
  jd_id: number;
  job_skills: Record<string, number>;
}
