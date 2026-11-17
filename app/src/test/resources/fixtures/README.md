# Response fixtures

These JSON files are **written from the client models** (`data/remote/model/response/**` and their
`@SerializedName`s), **not captured from the backend**. They pin what the app expects the API to send.
Replace them with real, anonymised responses (or generate them from the OpenAPI spec) when those are available:
the contract tests then start checking the server too.
