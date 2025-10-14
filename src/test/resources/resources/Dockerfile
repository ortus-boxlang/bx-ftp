FROM ubuntu

RUN apt update

RUN export DEBIAN_FRONTEND=noninteractive

RUN apt install -y vsftpd

RUN adduser test_user
RUN echo "test_user:testpass" | chpasswd

COPY ./vsftpd.conf /etc/vsftpd.conf 
COPY ./run.sh /run.sh 

CMD [ "sh", "/run.sh" ]
