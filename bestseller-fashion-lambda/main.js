/*
 * Copyright 2021 steadybit GmbH. All rights reserved.
 */

const failureLambda = require("failure-lambda");
const axios = require("axios");

exports.handler = failureLambda(async function (event, context) {
  let responseData = null;
  switch (event.httpMethod) {
    case "GET":
      if (event.path === "/remote-call") {
        const instance = axios.create({
          timeout: 3000,
        });
        const response = await instance.get(
          "https://raw.githubusercontent.com/steadybit/reliability-hub-db/main/index.json"
        );
        console.log(response.data);
        console.log(response.status);
        responseData = responseBuilder(response.status, response.data);
      } else {
        responseData = successResponseBuilder(
          JSON.stringify([
            {
              id: "118f5f84-6c3f-49fa-8c51-557aad443061",
              name: "Steadybit Hoodie",
              category: "FASHION",
              imageId: "hoodie",
              price: 69.99,
              availability: "AVAILABLE",
            },
            {
              id: "ad74d7e7-54f3-4936-a572-914fc60d5c4a",
              name: "Steadybit Sun Glasses",
              category: "FASHION",
              imageId: "sunglasses",
              price: 19.99,
              availability: "AVAILABLE",
            },
            {
              id: "949001dc-bd44-41e9-90c9-3820151d38b4",
              name: "Steadybit Socks",
              category: "FASHION",
              imageId: "socks",
              price: 9.99,
              availability: "AVAILABLE",
            },
          ])
        );
      }
      break;
    default:
      responseData = notImplemented();
  }
  return responseData;
});

const responseBuilder = (httpcode, body) => {
  return {
    statusCode: httpcode,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
    },
    body: body,
  };
};

const successResponseBuilder = (body) => {
  return responseBuilder(200, body);
};

const failureResponseBuilder = (statusCode, message) => {
  return responseBuilder(statusCode, JSON.stringify({ message: message }));
};

async function notImplemented() {
  return failureResponseBuilder(404, "Method not implemented!");
}
