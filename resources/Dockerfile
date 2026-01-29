FROM ubuntu

RUN apt update

RUN export DEBIAN_FRONTEND=noninteractive

# Install FTP and SSH servers
RUN apt install -y vsftpd openssh-server

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
