package tech.webclouds.simpledrivejava.services;

public interface IStorageService {

    /**
     * Saves a blob with the given ID, data, and content type.
     *
     * @param id          The unique identifier for the blob.
     * @param data        The blob data as a byte array.
     * @param contentType The MIME type of the blob.
     * @return True if the blob was successfully saved; otherwise, false.
     */
    boolean saveBlob(String id, byte[] data, String contentType);

    /**
     * Retrieves a blob by its ID and content type.
     *
     * @param id          The unique identifier for the blob.
     * @param contentType The expected MIME type of the blob.
     * @return The blob data as a byte array, or null if not found.
     */
    byte[] getBlob(String id, String contentType);
}
