# WorkMail Push Notifications

WorkMail’s Push Notifications API allows customers to receive notifications about changes in their mailbox, such as receiving a new mail and updates in the calendar, by using its Push Notifications feature. Customers can register URL’s (push notification responders) which will then receive these notifications. This allows the development of responsive client applications, as they can quickly reflect changes in the mailbox in the user interface.

This sample presents a demo Push Notification responder running on top of AWS Lambda and API Gateway, using the AWS Serverless framework.


This demo contains two parts:

* The first part consists of creating a subscription to the user’s mailbox. The subscription request consists of the URL of the push notification responder which will handle the notifications sent by the WorkMail service. The client responder is implemented using AWS Lambda (described in the second part).
* The second part is to implement the push notification responder which handles notifications sent by the WorkMail service.



The push notification responder (Lambda handler) performs the following activities:
* Receives the EWS XML notification and deserializes it.
* If the notification contains any CreateItem events it downloads the subject and the body of the item.
* If the subject contains a  keyword ('demo') then the body is passed to AWS Comprehend (An AWS Service - https://aws.amazon.com/comprehend/) to detect the language and perform sentiment analysis. 
* The result is sent back to the sender in the form of a reply.

## Prerequisites:
* git
* AWS CLI deployed and configured (https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) on your host.
* Java 8 with Maven.
* An S3 bucket which will be used to upload the code artifacts. (Referenced below as 'YOUR_S3_BUCKET').

To run the demo application perform the following operations:

## Steps

### Check out the code package
```
git clone ssh://git.amazon.com/pkg/GiraffePushNotificationsDemo

```

### Compile the code

```
mvn clean package shade:shade
```

### Package the code

```
aws cloudformation package --template-file template.yml --output-template-file output-template.yaml --s3-bucket <YOUR_S3_BUCKET_NAME>
```

### Deploy the code

```
aws cloudformation deploy --template-file output-template.yaml --stack-name workmail-demo --capabilities CAPABILITY_IAM
```

This will create a CloudFormation stack with two functions: one which will invoke the Push Subscription API  and another which will  handle the notifications.

Inspect  the output values of the cloud formation.

### Validation
```
aws cloudformation describe-stacks | jq '.Stacks[] | select(.StackName == "workmail-demo").Outputs[].OutputValue'
```

The first is the API Gateway endpoint of the lambda response handler and the second is the ARN of the lambda function which subscribes to the mailbox events.

### Credential Management

Both lambda functions require valid user credentials  to access the WorkMail endpoint. The code assumes that the user password is stored encrypted in the AWS Systems Manager Parameter Store (SSM Parameter store - https://docs.aws.amazon.com/systems-manager/latest/userguide/systems-manager-paramstore.html). 


To set the password in the SSM Parameter store, run the command below or use the AWS EC2 console to create the parameter. Note that the parameter name must match the user part of the email (e.g if the email address is john@example.com then the name of the parameter must be “john).


```
aws ssm put-parameter --name <USER> --value <PASSWORD> --type "SecureString"
```


To register a user's mailbox for push subscriptions run:
```
aws lambda invoke --function-name <SUBSCRIBE_LAMBDA_ARN> --payload  '{"email": "EMAIL"}' outputfile.txt
```

A successful run results in the following output in the console:

```
{
    "StatusCode": 200
}
```

To see the demo in action, send an email to the user subscribed for push subscriptions above with the subject "demo" from another user's account. The sender will then receive a reply consisting of the sentiment analysis of the content in the body of the initial email. 

To stop subscriptions just delete the stored password from the SSM Parameter store:
```
aws ssm delete-parameter --name <USER>
```

Once the parameter is deleted the lambda notification responder will respond with 'Unsubscribe' when the next notification will be received. 

To find out more about Amazon WorkMail, please visit  [Amazon WorkMail](https://aws.amazon.com/workmail/).

