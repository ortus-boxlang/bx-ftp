sleep 5s

service vsftpd start

echo "running"

service --status-all

tail -f /dev/null