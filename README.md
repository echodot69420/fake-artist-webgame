# Fake Artist Webgame

A minimal Hello World web server built with Python and FastAPI.

## Setup

Create and activate a virtual environment:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.bat
```

Install the dependencies:

```powershell
python -m pip install -r requirements.txt
```

## Launch

Start the development server from the project root:

```powershell
python -m uvicorn app.main:app --reload
```

Open <http://127.0.0.1:8000/> to see the Hello World response. Interactive API documentation is available at <http://127.0.0.1:8000/docs>.