From paytonod/mtgsmspricer
COPY src /project/src
COPY project.clj /project/project.clj
COPY serverless.yml /project/serverless.yml
COPY credentials /root/.aws/credentials
WORKDIR /project
RUN rm -rf /project/src/autograder
CMD sls deploy
