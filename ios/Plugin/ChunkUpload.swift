import Foundation

@objc public class ChunkUpload: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
    @objc public func uploadFile(path: String,
        url: String,
        headers: NSDictionary,
        onProgress: @escaping (Double) -> Void,
        onComplete: @escaping(String?) -> Void
        ) -> Void {

        let filePath = URL(string: path)?.path ?? "";
        guard let fileData = FileManager.default.contents(atPath: filePath) else {
            onComplete("Failed to load file at path: \(filePath)")
            return ;
        }
        // Array to store chunk IDs
        var chunkIds: [String] = []
        // Split file into 4MB chunks
        let chunkSize = 4 * 1024 * 1024 // 4MB
        let totalChunks = Int(ceil(Double(fileData.count) / Double(chunkSize)))
        let dispatchGroup = DispatchGroup()
        // Upload each chunk
        for i in 0..<totalChunks {
            dispatchGroup.enter()
            let rangeStart = i * chunkSize
            let rangeEnd = min((i+1) * chunkSize, fileData.count)
            let chunk = fileData.subdata(in: rangeStart..<rangeEnd)
            // Generate chunk ID
            let chunkId = UUID().uuidString.data(using: .utf8)?.base64EncodedString() ?? ""
            chunkIds.append(chunkId)
            // Append chunk ID to URL as query parameter
            let uploadChunkUrl = URL(string: "\(url)&comp=block&blockid=\(chunkId)")!
            // Upload chunk
            self.uploadChunk(chunk: chunk, url: uploadChunkUrl, headers: headers) { (error) in
                if let error = error {
                    //print("Failed to upload chunk: \(error)")
                    onComplete("Failed to upload chunk : \(error)")
                    return
                } else {
                    //=>> seem incorrect here, i goes wrong order
                    print(i);
                    print(totalChunks);
                    //calculate percentage of chunks processed
                    let percentage = Double((Double(i + 1) / Double(totalChunks)) * 100)
                    onProgress(percentage);
                    dispatchGroup.leave()
                }
            }
        }
        //commit the block ids
        dispatchGroup.notify(queue: .main) {
            let commitChunksUrl = URL(string: "\(url)&comp=blocklist")!
            //commit chunk
            self.commitChunks(blockIds: chunkIds, url: commitChunksUrl, headers: headers) { (error) in
                if let error = error {
                    onComplete("Failed to commit chunks: \(error)")
                } else {
                    onComplete(nil);
                }
            }
        }
        return ;
    }

    private func uploadChunk(chunk: Data, url: URL, headers: NSDictionary, completion: @escaping (Error?) -> Void) {
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        // Add headers
        for (key, value) in headers {
            if let key = key as? String, let value = value as? String {
                request.addValue(value, forHTTPHeaderField: key)
            }
        }
        // Set HTTP body
        request.httpBody = chunk
        // Create URL session and upload chunk
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            if let error = error {
                print("Failed to upload chunk: \(error)")
                completion(error)
            } else {
                print("Chunk uploaded successfully")
                completion(nil)
            }
        }
        task.resume()
    }
    
    private func commitChunks(blockIds: [String], url: URL, headers: NSDictionary, completion: @escaping (Error?) -> Void){
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        // Add headers
        for (key, value) in headers {
            if let key = key as? String, let value = value as? String {
                request.addValue(value, forHTTPHeaderField: key)
            }
        }
        
        // Set HTTP body
        let chunkIdsData = try? JSONEncoder().encode(blockIds)
        request.httpBody = chunkIdsData
        // Create URL session and post chunk IDs
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            if let error = error {
                print("Failed to post chunk IDs: \(error)")
                completion(error);
            } else {
                print("Chunk IDs posted successfully")
                completion(nil);
            }
        }
        
        task.resume()
    }
}
