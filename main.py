from fastapi import FastAPI, HTTPException, status
from p
ydantic import (BaseModel)
from typing import List

app = FastAPI(title="Schedule Service API")

class Lesson(BaseModel):
    id: int
    discipline: str
    teacher: str
    classroom: str
    time_slot: str

db_lessons = []

@app.get("/api/v1/lessons/", response_model=List[Lesson])
def get_lessons():
    return db_lessons

@app.get("/api/v1/lessons/{lesson_id}", response_model=Lesson)
def get_lesson(lesson_id: int):
    for lesson in db_lessons:
        if lesson.id == lesson_id:
            return lesson
    raise HTTPException(status_code=404, detail="Not found")

@app.post("/api/v1/lessons/", response_model=Lesson, status_code=status.HTTP_201_CREATED)
def create_lesson(lesson: Lesson):
    db_lessons.append(lesson)
    return lesson

@app.delete("/api/v1/lessons/{lesson_id}")
def delete_lesson(lesson_id: int):
    for index, lesson in enumerate(db_lessons):
        if lesson.id == lesson_id:
            db_lessons.pop(index)
            return {"message": "Deleted"}
    raise HTTPException(status_code=404, detail="Not found")