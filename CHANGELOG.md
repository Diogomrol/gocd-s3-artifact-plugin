### 2.0.0 (2019-May-19)

Features and improvements:
 - Added numerous tests at unit and integration level
 - Implemented pattern maching and directory uploads
 - Implemented fetch of multiple files or directories
 - Implemented environment variable expansion in the destination prefix of the S3 bucket

Build system and release cycle:
 - Updated build system to use gradle 5.x
 - Added builds in docker using dojo.
 - Automated testing and releases
 - Added integration tests with AWS S3 bucket, publish and fetch cycle

### 1.0.0 (2018-08-31)

- Support the creation of an artifact store for AWS S3 using AWS Credentials (AWS Access Key and AWS Secret Access Key)
- Support for publishing file artifacts to S3 (not folders or wildcard paths)
- Support for fetching files from S3
