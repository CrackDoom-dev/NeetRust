from typing import Dict, List, Optional
from pydantic import BaseModel

class SourceInfo(BaseModel):
    id: str
    name: str
    base_url: str
    preset: str = "wp-anime"
    enabled: bool = True
    priority: int = 100
    builtin: bool = False
    healthy: bool = True
    fail_count: int = 0
    cooldown_remaining: int = 0
    last_error: Optional[str] = None

class SourceCreate(BaseModel):
    name: Optional[str] = None
    base_url: str
    id: Optional[str] = None
    preset: str = "wp-anime"
    urls: Dict[str, str] = {}
    selectors: Dict[str, str] = {}

class SourceUpdate(BaseModel):
    name: Optional[str] = None
    base_url: Optional[str] = None
    enabled: Optional[bool] = None
    urls: Optional[Dict[str, str]] = None
    selectors: Optional[Dict[str, str]] = None

class SourceOrder(BaseModel):
    ids: List[str]

class ProbeOp(BaseModel):
    ok: bool
    count: int = 0
    error: Optional[str] = None

class ProbeResult(BaseModel):
    ok: bool
    ops: Dict[str, ProbeOp] = {}
    summary: str = ""

class SourceAddResult(BaseModel):
    source: SourceInfo
    probe: Optional[ProbeResult] = None
