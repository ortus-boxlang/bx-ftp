FROM gradle:8.7-jdk21-jammy
RUN apt update

RUN export DEBIAN_FRONTEND=noninteractive

RUN apt install -y vsftpd

RUN adduser test_user
RUN echo "test_user:testpass" | chpasswd

COPY ./resources/vsftpd.conf /etc/vsftpd.conf 

RUN service vsftpd start

COPY ./ /app

USER root

WORKDIR /app

RUN chown -R gradle:gradle ./

RUN rm -rf .gradle
RUN rm -rf build

RUN gradle clean
RUN gradle :downloadBoxLang

RUN gradle :test