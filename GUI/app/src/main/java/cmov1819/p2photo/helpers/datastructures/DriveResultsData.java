package cmov1819.p2photo.helpers.datastructures;

import java.util.ArrayList;

public class DriveResultsData {
    private String folderId;
    private String fileId;
    private String fileUrl;
    private ArrayList<String> folderContents;
    private boolean hasError = false;

    public DriveResultsData(){}

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

    public ArrayList<String> getFolderContents() {
        return folderContents;
    }

    public void setFolderContents(ArrayList<String> folderContents) {
        this.folderContents = folderContents;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }
}
