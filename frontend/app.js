/* Shared logic: storage, helpers and the New Task popup (used by index.html and day.html) */

const pad = n => String(n).padStart(2, "0");
const dateKey = (y, m, d) => `${y}-${pad(m + 1)}-${pad(d)}`;
const todayKey = () => { const n = new Date(); return dateKey(n.getFullYear(), n.getMonth(), n.getDate()); };
const $ = id => document.getElementById(id);

/* ---------- Storage (localStorage stands in for a database so both pages share data) ---------- */
const Store = {
    load() {
        try { return JSON.parse(localStorage.getItem("taskAppData")) || { calendar: {}, backend: [] }; }
        catch { return { calendar: {}, backend: [] }; }
    },
    save(data) {
        try { localStorage.setItem("taskAppData", JSON.stringify(data)); } catch {}
    }
};

/* Replace with a real fetch() to your API when the back-end exists. */
function saveTaskToBackend(task) {
    console.log("Saved to back-end:", task);
    const data = Store.load();
    data.backend.push(task);
    Store.save(data);
    // fetch("/api/tasks", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(task) });
}

function taskDurationPredict(task){
    var duration_pred_class = Java.type("com.sivajavatechie.script.DurationPredictor")
    return duration_pred_class.predictTime(task.description)
}

/* ---------- Popup ---------- */
const TaskPopup = {
    onSaved: null, // pages set this to re-render after a save

    init() {
        document.body.insertAdjacentHTML("beforeend", `
      <div class="overlay" id="overlay">
        <div class="popup" role="dialog" aria-modal="true" aria-labelledby="popupTitle">
          <h3 id="popupTitle">New Task</h3>
          <label for="desc">Description</label>
          <textarea id="desc" placeholder="What needs doing?"></textarea>
          <div class="row">
            <div><label for="date">Date (optional)</label><input type="date" id="date"></div>
            <div><label for="time">Time (optional)</label><input type="time" id="time"></div>
          </div>
          <label for="durHours">Duration (optional)</label>
          <div class="row">
            <div><input type="number" id="durHours" min="0" max="23" placeholder="Hours"></div>
            <div><input type="number" id="durMins" min="0" max="59" placeholder="Minutes"></div>
          </div>
          <p class="hint">Tasks with a date appear on the calendar. Tasks without one are saved to the back-end.</p>
          <div class="actions">
            <button id="cancelBtn">Cancel</button>
            <button class="primary" id="saveBtn">Save</button>
          </div>
        </div>
      </div>
      <div id="toast"></div>`);
        $("cancelBtn").addEventListener("click", () => this.close());
        $("saveBtn").addEventListener("click", () => this.save());
        $("overlay").addEventListener("click", e => { if (e.target === $("overlay")) this.close(); });
        document.addEventListener("keydown", e => { if (e.key === "Escape") this.close(); });
    },

    open(date = "") {
        $("desc").value = "";
        $("date").value = date;
        $("time").value = "";
        $("durHours").value = "";
        $("durMins").value = "";
        $("overlay").classList.add("open");
        $("desc").focus();
    },

    close() { $("overlay").classList.remove("open"); },

    toast(msg) {
        const t = $("toast");
        t.textContent = msg;
        t.classList.add("show");
        setTimeout(() => t.classList.remove("show"), 2200);
    },

    save() {
        const description = $("desc").value.trim();
        if (!description) { $("desc").focus(); return; }

        const hours = Math.max(parseInt($("durHours").value, 10) || 0, 0);
        const mins = Math.max(parseInt($("durMins").value, 10) || 0, 0);

        const task = {
            id: Date.now(),
            description,
            date: $("date").value || null,
            time: $("time").value || null,
            duration: hours * 60 + mins || null // total minutes (e.g. 1h 20m = 80)
        };

        if (task.date) {
            if(task.duration){
                const data = Store.load();
                (data.calendar[task.date] ||= []).push(task);
                Store.save(data);
                this.toast("Task added to calendar");
            }
            else{
                task.duration = taskDurationPredict(task);
                const data = Store.load();
                (data.calendar[task.date] ||= []).push(task);
                Store.save(data);
                this.toast("Task duration predicted and added to calendar");
            }

        } else {
            saveTaskToBackend(task);
            this.toast("Task saved (no date)");
        }
        this.close();
        if (this.onSaved) this.onSaved(task);
    }
};