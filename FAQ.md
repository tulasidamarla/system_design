- Horizontal Partitioning
- Vertical partitioning  
- Directory based partition

## what are different horizontal partitioning schemes?

- `Range partitioning:` This type of partitioning divides the data into ranges based on a column value. 
  For example, you could partition a table of customer orders by the customer's zip code.
- `List partitioning:` This type of partitioning divides the data into lists based on a column value. 
  For example, you could partition a table of products by the product category.
- `Hash partitioning:` This type of partitioning divides the data into buckets based on a hash of a column value. 
  This ensures that the data is evenly distributed across the partitions.
- `Composite partitioning:` This type of partitioning combines two or more of the above partitioning schemes. 
  For example, you could combine range partitioning with list partitioning to divide the data into ranges based 
  on the customer's zip code and then into lists based on the product category.

## which partition scheme to use?

- The best type of horizontal partition scheme to use will depend on the specific requirements of your application. 
  - For example, if you need to ensure that the data is evenly distributed across the partitions, 
  then you should use hash partitioning. 
  - If you need to be able to quickly query for data within a specific range, then you should use range partitioning.

## what are the advantages and disadvantages of using horizontal partitioning:

- `Advantages`
  - `Improved performance:` Horizontal partitioning can improve the performance of queries by reducing the amount of 
    data that needs to be scanned.
  - `Increased scalability:` Horizontal partitioning can help to improve the scalability of a database by distributing 
    the data across multiple servers.
  - `Reduced data redundancy:` Horizontal partitioning can help to reduce data redundancy by storing duplicate data 
    only once.
  - `Simplified administration:` Horizontal partitioning can simplify the administration of a database by making it 
    easier to manage and maintain the data.
- `Disadvantages`
  - `Increased complexity:` Horizontal partitioning can increase the complexity of a database by making it more 
    difficult to design and implement.
  - `Increased network traffic:` Horizontal partitioning can increase the network traffic between the servers that 
    store the data.
  - `Challenges with joins:` Horizontal partitioning can make it more difficult to perform joins between tables.
