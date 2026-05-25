package com.solutions.alphil.zambiajobalerts.classes;

import android.app.Application;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.solutions.alphil.zambiajobalerts.JobLaunchParser;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JobRepository {
    public static final int DEFAULT_PAGE_SIZE = 30;
    private static final String TOTAL_PAGES_HEADER = "X-WP-TotalPages";

    private final JobDao jobDao;
    private final LiveData<List<JobEntity>> allJobs;
    private final RequestQueue queue;
    private final Gson gson = new Gson();
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);

    public JobRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        jobDao = db.jobDao();
        allJobs = jobDao.getAllJobs();
        queue = Volley.newRequestQueue(application);
    }

    public LiveData<List<JobEntity>> getAllJobs() {
        return allJobs;
    }

    public LiveData<List<JobEntity>> searchJobs(String query) {
        return jobDao.searchJobs(query);
    }

    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }

    public void refreshJobs() {
        loadJobsPage(1, DEFAULT_PAGE_SIZE, null);
    }

    public void loadJobsPage(int page, int perPage, ResponseListener<JobsPageResult> listener) {
        if (Boolean.TRUE.equals(isRefreshing.getValue())) return;
        setRefreshing(true);

        int safePage = Math.max(1, page);
        int safePerPage = Math.max(1, perPage);
        final int[] totalPages = {-1};
        String url = ApiConfig.WP_JOB_LISTINGS_URL
                + "?per_page=" + safePerPage
                + "&page=" + safePage
                + "&_embed=1";
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        Type listType = new TypeToken<List<Job>>(){}.getType();
                        List<Job> jobs = gson.fromJson(response, listType);
                        List<JobEntity> entities = new ArrayList<>();
                        for (Job job : jobs) {
                            entities.add(mapToEntity(job));
                        }
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            jobDao.insertJobs(entities);
                            isRefreshing.postValue(false);
                            if (listener != null) {
                                boolean hasMore = totalPages[0] > 0
                                        ? safePage < totalPages[0]
                                        : entities.size() >= safePerPage;
                                listener.onResponse(new JobsPageResult(safePage, entities, hasMore));
                            }
                        });
                    } catch (Exception e) {
                        Log.e("JobRepository", "Error parsing jobs: " + e.getMessage());
                        setRefreshing(false);
                        if (listener != null) {
                            listener.onError("Parse error: " + e.getMessage());
                        }
                    }
                },
                error -> {
                    String message = error.getMessage() != null ? error.getMessage() : "Unable to fetch jobs";
                    Log.e("JobRepository", "Error fetching jobs: " + message);
                    setRefreshing(false);
                    if (listener != null) {
                        listener.onError(message);
                    }
                }) {
            @Override
            protected Response<String> parseNetworkResponse(NetworkResponse response) {
                totalPages[0] = parseIntHeader(response.headers, TOTAL_PAGES_HEADER, -1);
                return super.parseNetworkResponse(response);
            }
        };

        queue.add(request);
    }

    private void setRefreshing(boolean refreshing) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            isRefreshing.setValue(refreshing);
        } else {
            isRefreshing.postValue(refreshing);
        }
    }

    public void fetchJobDetails(int jobId, ResponseListener<JobEntity> listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            JobEntity localJob = jobDao.getJobById(jobId);
            if (localJob != null) {
                listener.onResponse(localJob);
            } else {
                String url = ApiConfig.WP_JOB_LISTINGS_URL + "/" + jobId + "?_embed";
                fetchJobFromUrl(url, listener);
            }
        });
    }

    public void fetchJobDetailsBySlug(String slug, ResponseListener<JobEntity> listener) {
        String url = JobLaunchParser.buildJobDetailsBySlugUrl(ApiConfig.WP_JOB_LISTINGS_URL, slug);
        fetchJobFromUrl(url, new ResponseListener<List<JobEntity>>() {
            @Override
            public void onResponse(List<JobEntity> result) {
                if (result != null && !result.isEmpty()) {
                    listener.onResponse(result.get(0));
                } else {
                    listener.onError("Job not found");
                }
            }

            @Override
            public void onError(String error) {
                listener.onError(error);
            }
        }, new TypeToken<List<Job>>(){}.getType());
    }

    private void fetchJobFromUrl(String url, ResponseListener<JobEntity> listener) {
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        Job job = gson.fromJson(response, Job.class);
                        JobEntity entity = mapToEntity(job);
                        AppDatabase.databaseWriteExecutor.execute(() -> jobDao.insertJobs(List.of(entity)));
                        listener.onResponse(entity);
                    } catch (Exception e) {
                        listener.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> listener.onError(error.getMessage() != null ? error.getMessage() : "Unable to fetch job details"));
        queue.add(request);
    }

    private <T> void fetchJobFromUrl(String url, ResponseListener<List<JobEntity>> listener, Type type) {
        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        List<Job> jobs = gson.fromJson(response, type);
                        List<JobEntity> entities = new ArrayList<>();
                        for (Job job : jobs) {
                            entities.add(mapToEntity(job));
                        }
                        AppDatabase.databaseWriteExecutor.execute(() -> jobDao.insertJobs(entities));
                        listener.onResponse(entities);
                    } catch (Exception e) {
                        listener.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> listener.onError(error.getMessage() != null ? error.getMessage() : "Unable to fetch job details"));
        queue.add(request);
    }

    private JobEntity mapToEntity(Job job) {
        return new JobEntity(
                job.getId(), job.getTitle(), job.getExcerpt(), job.getContent(),
                job.getDate(), job.getLink(), job.getFeaturedImage(),
                job.getCompany(), job.getLocation(), job.getJobType(), job.getApplication()
        );
    }

    private int parseIntHeader(Map<String, String> headers, String headerName, int defaultValue) {
        if (headers == null) {
            return defaultValue;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(headerName)) {
                try {
                    return Integer.parseInt(entry.getValue());
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }

        return defaultValue;
    }

    public static final class JobsPageResult {
        private final int page;
        private final List<JobEntity> jobs;
        private final boolean hasMore;

        public JobsPageResult(int page, List<JobEntity> jobs, boolean hasMore) {
            this.page = page;
            this.jobs = jobs;
            this.hasMore = hasMore;
        }

        public int getPage() {
            return page;
        }

        public List<JobEntity> getJobs() {
            return jobs;
        }

        public boolean hasMore() {
            return hasMore;
        }
    }

    public interface ResponseListener<T> {
        void onResponse(T result);
        void onError(String error);
    }
}
