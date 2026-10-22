FROM ubuntu:latest
LABEL authors="saoir"

ENTRYPOINT ["top", "-b"]