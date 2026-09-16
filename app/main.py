from fastapi import FastAPI

app = FastAPI(title="Fake Artist Webgame")


@app.get("/")
def read_root() -> dict[str, str]:
    return {"message": "Hello, world!"}
