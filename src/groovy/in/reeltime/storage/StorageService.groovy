package in.reeltime.storage

interface StorageService {

    void store(InputStream inputStream, String basePath, String resourcePath)
}
