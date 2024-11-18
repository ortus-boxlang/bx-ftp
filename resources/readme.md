# A BoxLang Module to Add FTP Support

## Testing

Testing this module requires a little bit of setup. You will need to provide a working ftp server.

To configure environment variables for your specifc setup copy the file `src/test/resources/.env.example` to `src/test/resources/.env` and fill in the values.

After that run this command to setup the directory structure of the ftp server.

```
# replace /home/test_user with the location of your FTP folder
cp -rT ./resources/ftp_files /home/test_user
```

You should now be ready to run your tests.