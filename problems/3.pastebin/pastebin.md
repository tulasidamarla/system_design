## what is paste bin and why do we need that?

- Paste bin service enable users to store plain text, code samples, logs or images and generate unique URLs to access 
  the uploaded data.

## Requirements

### _Functional_

- Users should be able to upload or “paste” their data and get a unique URL to access it.
- Users will only be able to upload text.
- Data and links will expire after a specific time span automatically; Can specify expiration time in the request.
- Users should optionally be able to pick a custom alias for their paste.

### _Non Functional_

- The system should be highly reliable, any data uploaded should not be lost.
- The system should be highly available.
- Users should be able to access their Pastes in real-time with minimum latency.
- Paste links should not be predictable.

### _Extended Requirements_

- how many times a paste was accessed?(Analytics)
- The service should also be accessible through REST APIs by other services.

## Back of the envelope estimation

- The system will be read-heavy. Let’s assume 5:1 ratio between read and write
- Assume 1 million create pastes per day.
- Assume a max size limit of 10MB per paste.

### _Traffic estimates_

```
No of pastes per second 1M / 86400 = 12 pastes/sec
No of reads per sec 5M / 86400 = 58 per sec(approx)
```
### _Storage estimates_

```
# Storage for pastes
- Assume an average size of 10KB per paste.
- Total storage required per day 1M * 10KB = 10GB
- Total storage for pastes10 years = 10GB * 365 * 10 = 36500GB = 36.5TB
- To keep some buffer by assuming 70% capacity model, total storage required is approx 51TB.(51*0.7TB = 36TB)

# Storage for keys
- Total no of pastes for 10 years = 1M * 365 * 10 = 3650M = 3.65Billion
- Choosing base64 encoding gives 68.7Billion(64^6) unique strings for 6 characters
- Assuming 6 bytes for 6 chars, total memory required is 6bytes * 3.65B = 22GB
```

### _Bandwidth estimates_

```
- For writing 12 pastes per sec, 12 * 10KB = 120KB/S
- For reading 58 pastes per sec, 58 * 10KB = 580KB/S or 0.6MB/S
```

### _Memory estimates_

```
- With 5M read requests per day, following 80-20 rule(20 percent of pastes generates 80 percent traffic)
  0.2 * 5M * 10KB = 10GB
```

## API design

```
addPaste(api_dev_key, paste_data, custom_url=None, user_name=None, expire_date=None) -> :string(returns url)
getPaste(api_dev_key, paste_key)
deletePaste(api_dev_key, paste_key)
```

## Database design

- `Considerations`
  - Need to store billions of records
  - Each metadata object is small(less than 100 bytes)
  - Each paste object is of medium size(less than 10MB)  
  - No relationship between records(except storing info about user and created paste)
  - Service is read-heavy

<img src="images/pastebin_db_model.png" height="300" width="400"/>

- URLHash is the generated pastebin URL hash.
- ContentKey is the object key(usually either amazon s3 URL, google cloud storage URL) storing the contents of the paste.

## High level design

- An application layer will serve read and write requests.
- Application layer talks to storage layer to store and retrieve data.
- The data model contains two storage systems.
  - `Metadata storage:` Stores data related to each paste.
  - `Object storage:` Stores the content of the pastes.
  
<img src="images/pastebin_hld.png" height="300" width="400"/>

## Component design(Low level design)

### _Application Layer_

- `Handling write requests`
  - Up on receiving write request, app server generates random six-letter string.
  - App server stores the contents of the paste via storage service and generated key in the db.
  - After successful insertion, the server returns the key.
  - `Problems`
    - Insertion may fail because of a duplicate key.
  - `Solution`
    - `Generating keys offline`
    - A standalone Key Generation Service (KGS) that generates random six letter strings beforehand.
    - Store the keys in a database and ensure no duplicates.
    - When a request comes for creation, a key is fetching from already generated keys.
    - `Advantages`
      - No need to worry about duplicates
      - No need to encode URLs
    - `Considerations`
      - `How to handle concurrent requests from app server to KGS`
        - KGS can use two tables: one for already used keys and another for not used.
        - As soon as keys are given to one of the app servers, move them to used keys table.
        - KGS can load some keys into memory beforehand and move them to used keys table for better performance.
        - If KGS dies before handling the keys to app servers, few keys are wasted. But, that should be fine given
          huge number of keys(68.7B), which is considerably higher than required(3.6B)
      - `Key DB size`
        - `6(chars) * 68.7B = 412 GB`
      - `Single point of failure`
        - Have a stand-alone replica of KGS.
      - `Cache some keys at app server`
        - Caching some keys at app server by fetching from KGS improves performance.
        - If an app server dies, then few keys will be wasted. But, that should be acceptable since we have 68.7B keys.
      - `Performing key lookup`
        - If key is present in DB send a 302 redirect status back to the browser by passing stored URL in the location
          header.
        - If key is not present send a 404 status or redirect back to the home page.
      - The below python code generated 1 million keys in .35 seconds in my laptop.

```python
def generate_random_string(n):
    letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    used_strings = set()

    while len(used_strings) < n:
        string = ''.join(random.choice(letters) for _ in range(6))
        if string not in used_strings:
            used_strings.add(string)
            yield string
```

### _Datastore layer_

- `Metadata database:`
  - A relational db like MySQL or a distributed key-value store like Dynamo or cassandra can be used.
- `Object storage:`
  - An object storage service like Amazon S3.

### _Data partitioning and replication_

- DB needs to be scaled for storing billions of pastes metadata.
- Scaling requires a partitioning scheme to store data to different DB servers.
  - `Range based partitioning`
    - Store URLs based on the first letter of the hash key.
    - Some less frequently letters can be combined.
    - `Problems`
      - Can lead to unbalanced servers due to hotspots.
  - `Hash based partitioning`
    - Calculate the hash of key to determine the partition.
    - This approach can still lead to hotspot especially when servers are added and removed.
    - The solution for the hotspots is to use `consistent-hasing`.

### _Cache_

- The system can use Memcache or redis, which can store pastes with their respective keys.
- Caching can be used for both the metadata and the pastes.  
- The application servers, before hitting object storage, can quickly check if the cache has the desired paste.
- Cache for pastes requires 10 GB. Most modern servers support 256 GB, all the data can fit in one server.
  - Alternately, we can use a couple of smaller servers for better read throughput.
- `Cache eviction policy`
  - Least Recently Used (LRU) can be a reasonable policy for this system.
    - For custom-built caches, using a data structure like linked hash map can achieve this.
    - For systems like memcached and redis, various configuration options are provided to choose LRU.
- `Handling cache miss`
  - Whenever there is a cache miss, app servers would be hitting a backend object storage.
  - Update the cache and pass the new entry to all cache replicas.
  - Each replica can update their cache by adding the new entry.

### _Load balancer_

- Load balancing layer can be placed at three places in the system:
  - Between client and app servers
  - Between app servers and database servers
  - Between app servers and cache servers(both metadata and block cache)
- Strategies
  - `round-robin`
    - Simple to implement and doesn't introduce over head.
    - Doesn't take server load into consideration.
  - `weighted round-robin`
    - Server load is taken into consideration before sending request to a server.
    - Puts over head on the load balancer.
- Other strategies like least connection, least response time, iphash etc.

### _DB cleanup_

- If a user specified expiration time or default expiration time is reached, entries should be removed from DB.
- A backend process(cleanup service) should make sure that expired links(or hashes) are removed.
- After removing the expired links, the keys should be put back into key db.
- For object storage life cycle policies can be setup, which are automatically removed by cloud provider.

<img src="images/pastebin_design.png" height="300" width="400"/>

- `Note:` Load balancer service is not required for external object storage like s3.

### _Security and Permissions_

- Private URLs can be created to allow access to certain users.
- There are two ways to this.
  - Store permission level (public/private) with each paste in the database.
  - Create a separate table to store UserIDs that have permission to see a specific URL.
    - Storing the data in a column wide DB like cassandra with key is the hash and columns are the list of userIds
      which have access.
- If a user does not have permission and tries to access a URL, send an error (HTTP 401) back.      