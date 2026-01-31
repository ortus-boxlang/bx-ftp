FROM ubuntu

# Update package lists and install FTP and SSH servers
RUN apt-get update && \
	DEBIAN_FRONTEND=noninteractive apt-get install -y vsftpd openssh-server && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

# Create test user for FTP and SFTP
RUN adduser test_user
RUN echo "test_user:testpass" | chpasswd

# Configure vsftpd for FTP
COPY ./vsftpd.conf /etc/vsftpd.conf

# Configure SSH for SFTP
RUN mkdir -p /var/run/sshd
RUN echo "PasswordAuthentication yes" >> /etc/ssh/sshd_config
RUN echo "PermitRootLogin no" >> /etc/ssh/sshd_config
RUN echo "Subsystem sftp /usr/lib/openssh/sftp-server" >> /etc/ssh/sshd_config

# Startup script
COPY ./run.sh /run.sh

CMD [ "sh", "/run.sh" ]
