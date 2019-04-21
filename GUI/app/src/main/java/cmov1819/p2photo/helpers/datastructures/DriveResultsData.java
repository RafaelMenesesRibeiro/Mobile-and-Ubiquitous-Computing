package cmov1819.p2photo.helpers.datastructures;

import android.content.Intent;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class DriveResultsData {
    private ArrayList<String> folderContents = null;
    private Intent suggestedIntent = null;
    private String folderId = null;
    private String fileId = null;
    private String fileUrl = null;
    private String message = null;
    private Boolean hasError = false;
    private Boolean suggestRetry = false;
    private AtomicInteger attempts = new AtomicInteger(0);

    public DriveResultsData(){}

    public ArrayList<String> getFolderContents() {
        return folderContents;
    }

    public void setFolderContents(ArrayList<String> folderContents) {
        this.folderContents = folderContents;
    }

    public Intent getSuggestedIntent() {
        return suggestedIntent;
    }

    public void setSuggestedIntent(Intent suggestedIntent) {
        this.suggestedIntent = suggestedIntent;
    }

    public String getFolderId() {
        return folderId;
    }

    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getHasError() {
        return hasError;
    }

    public void setHasError(Boolean hasError) {
        this.hasError = hasError;
    }

    public Boolean getSuggestRetry() {
        return suggestRetry;
    }

    public void setSuggestRetry(Boolean suggestRetry) {
        this.suggestRetry = suggestRetry;
    }

    public Integer getAttempts() {
        return attempts.getAndIncrement();
    }

}
