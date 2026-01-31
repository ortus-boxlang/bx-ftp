sleep 5s

# Start FTP server
service vsftpd start

# Start SSH server for SFTP
service ssh start

echo "running"

service --status-all

tail -f /dev/null