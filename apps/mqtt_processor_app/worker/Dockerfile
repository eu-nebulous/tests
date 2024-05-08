FROM python:3.11
RUN mkdir /app
WORKDIR /app
COPY ./requirements.txt /app/requirements.txt
RUN pip install -r /app/requirements.txt
COPY ./worker.py ./worker.py
CMD [ "python3","-u", "./worker.py"]