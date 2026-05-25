package com.solutions.alphil.zambiajobalerts.ui.jobs;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.solutions.alphil.zambiajobalerts.classes.Job;
import com.solutions.alphil.zambiajobalerts.classes.JobEntity;
import com.solutions.alphil.zambiajobalerts.classes.JobRepository;

import java.util.ArrayList;
import java.util.List;

public class JobsViewModel extends AndroidViewModel {
    private final JobRepository repository;
    private final LiveData<List<Job>> jobsLiveData;
    private final MutableLiveData<String> searchTerm = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasMoreData = new MutableLiveData<>(true);
    private final MutableLiveData<Job> jobDetailsLiveData = new MutableLiveData<>();
    private int currentPage = 0;
    private volatile boolean requestInFlight = false;

    public JobsViewModel(@NonNull Application application) {
        super(application);
        repository = new JobRepository(application);
        
        LiveData<List<JobEntity>> entitiesLiveData = Transformations.switchMap(searchTerm, query -> {
            if (query == null || query.isEmpty()) {
                return repository.getAllJobs();
            } else {
                return repository.searchJobs(query);
            }
        });

        jobsLiveData = Transformations.map(entitiesLiveData, entities -> {
            List<Job> jobs = new ArrayList<>();
            if (entities != null) {
                for (JobEntity entity : entities) {
                    jobs.add(Job.fromEntity(entity));
                }
            }
            return jobs;
        });

        refreshJobs();
    }

    public LiveData<List<Job>> getJobs() { return jobsLiveData; }
    public LiveData<Boolean> getLoading() { return loadingLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<Boolean> getHasMoreData() { return hasMoreData; }
    public LiveData<Job> getJobDetails() { return jobDetailsLiveData; }

    public void refreshJobs() {
        loadPage(1);
    }

    public void loadJobs(boolean forceRefresh) {
        if (forceRefresh || currentPage == 0) {
            refreshJobs();
        }
    }

    public void loadLimitedJobs(int limit) {
        refreshJobs();
    }

    public void searchJobs(String query) {
        searchTerm.setValue(query);
    }
    
    public void loadMoreJobs() {
        Boolean hasMore = hasMoreData.getValue();
        if (Boolean.FALSE.equals(hasMore)) {
            return;
        }

        int nextPage = currentPage < 1 ? 1 : currentPage + 1;
        loadPage(nextPage);
    }

    private void loadPage(int page) {
        if (requestInFlight) {
            return;
        }

        requestInFlight = true;
        loadingLiveData.setValue(true);
        errorLiveData.setValue(null);

        repository.loadJobsPage(page, JobRepository.DEFAULT_PAGE_SIZE, new JobRepository.ResponseListener<JobRepository.JobsPageResult>() {
            @Override
            public void onResponse(JobRepository.JobsPageResult result) {
                currentPage = result.getPage();
                hasMoreData.postValue(result.hasMore());
                loadingLiveData.postValue(false);
                requestInFlight = false;
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
                loadingLiveData.postValue(false);
                requestInFlight = false;
            }
        });
    }

    public void fetchJobDetails(int jobId) {
        loadingLiveData.setValue(true);
        repository.fetchJobDetails(jobId, new JobRepository.ResponseListener<JobEntity>() {
            @Override
            public void onResponse(JobEntity result) {
                jobDetailsLiveData.postValue(Job.fromEntity(result));
                loadingLiveData.postValue(false);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
                loadingLiveData.postValue(false);
            }
        });
    }

    public void fetchJobDetailsBySlug(String slug) {
        loadingLiveData.setValue(true);
        repository.fetchJobDetailsBySlug(slug, new JobRepository.ResponseListener<JobEntity>() {
            @Override
            public void onResponse(JobEntity result) {
                jobDetailsLiveData.postValue(Job.fromEntity(result));
                loadingLiveData.postValue(false);
            }

            @Override
            public void onError(String error) {
                errorLiveData.postValue(error);
                loadingLiveData.postValue(false);
            }
        });
    }
}
