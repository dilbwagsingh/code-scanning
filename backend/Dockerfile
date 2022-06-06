FROM node:14-alpine3.10

ARG PORT_ARG=3001
ARG MONGODB_URL_ARG="mongodb://mongo:27017/electronicVoting"

WORKDIR /app

ENV PORT=${PORT_ARG}
ENV MONGODB_URL=${MONGODB_URL_ARG}

COPY ./package*.json ./

RUN npm install

COPY . .

EXPOSE 3001

CMD [ "npm", "start" ]