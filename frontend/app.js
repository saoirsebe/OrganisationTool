/* Shared logic: storage, helpers and the New Task popup */

const pad = n => String(n).padStart(2, "0");
const dateKey = (y, m, d) => `${y}-${pad(m + 1)}-${pad(d)}`;
const todayKey = () => { const n = new Date(); return dateKey(n.getFullYear(), n.getMonth(), n.getDate()); };
const $ = id => document.getElementById(id);



async function saveTaskToBackend(task) {
    const res = await fetch("http://localhost:3000/api/tasks", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            userId: task.userId,
            description: task.description,
            date: task.date,
            time: task.time,
            duration: task.duration,
            dateIsFixed: task.dateIsFixed,
            timeIsFixed: task.timeIsFixed,
            deadline: task.deadline,
            priority: task.priority
        }),
    });
    if (!res.ok) throw new Error("Save failed");
    return await res.json();   // the saved task, including its database id
}

async function taskDurationPredict(task) {
    const response = await fetch('http://localhost:3000/api/task_duration_prediction', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ description: task.description })
    });

    if (!response.ok) {
        throw new Error('Prediction request failed');
    }

    const { predictedMinutes } = await response.json();
    return predictedMinutes;
}

/* ---------- Popup ---------- */
const TaskPopup = {
    onSaved: null, // pages set this to re-render after a save

    init() {
        document.body.insertAdjacentHTML("beforeend", `
      <div class="overlay" id="overlay">
        <div class="popup" role="dialog" aria-modal="true" aria-labelledby="popupTitle">
          <h3 id="popupTitle">New Task</h3>
          
          <label for="desc">Description *</label>
          <textarea id="desc" placeholder="What needs doing?"></textarea>
          
          <div class="row">
            <div><label for="date">Date (optional)</label><input type="date" id="date"></div>
            <div><label for="time">Time (optional)</label><input type="time" id="time"></div>
          </div>
          
          <div class="row">
            <label for="priority">Priority *</label>
            <select id="priority">
              <option value="1">High</option>
              <option value="2" selected>Medium</option>
              <option value="3">Low</option>
            </select>
          </div>
      
          <div class="row fixed-options">
            <div>
                <label for="dateIsFixed">Date is fixed</label>
                <input type="checkbox" id="dateIsFixed">
            </div>
        
            <div>
                <label for="timeIsFixed">Time is fixed</label>
                <input type="checkbox" id="timeIsFixed">
            </div>
          </div>
          
          <label for="durHours">Duration (optional)</label>
          <div class="row">
            <div><input type="number" id="durHours" min="0" max="23" placeholder="Hours"></div>
            <div><input type="number" id="durMins" min="0" max="59" placeholder="Minutes"></div>
          </div>
          
          <label for="deadlineDate">Task deadline (Optional)</label>
          <div class="row">
            <div><label for="deadlineDate">Date </label><input type="date" id="deadlineDate"></div>
            <div><label for="deadlineTime">Time (optional)</label><input type="time" id="deadlineTime"></div>
          </div>
          
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
        $("dateIsFixed").checked = false;
        $("timeIsFixed").checked = false;
        $("priority").value = "";
        $("deadlineDate").value = "";
        $("deadlineTime").value = "";

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

    async save() {
        const description = $("desc").value.trim();
        if (!description) {
            $("desc").focus();
            return;
        }
        const priority = $("priority").value;
        if (!priority) {
            $("priority").focus();
            return;
        }

        const hours = Math.max(parseInt($("durHours").value, 10) || 0, 0);
        const mins = Math.max(parseInt($("durMins").value, 10) || 0, 0);

        const deadlineDate = $("deadlineDate").value;
        const deadlineTime = $("deadlineTime").value;
        const deadline = deadlineDate
            ? `${deadlineDate}T${deadlineTime || '23:59'}`
            : null;

        const task = {
            id: Date.now(),
            description,
            userId: 1, //PLACEHOLDER for real userId
            date: $("date").value || null,
            time: $("time").value || null,
            duration: hours * 60 + mins || null, // total minutes (e.g. 1h 20m = 80)
            dateIsFixed: $("dateIsFixed").checked,
            timeIsFixed: $("timeIsFixed").checked,
            deadline: deadline || null,
            priority: $("priority").value
        };

        if (!task.duration) {
            const durationPrediction = await taskDurationPredict(task);
            task.duration = Math.round(durationPrediction);
            this.toast("Task duration predicted");
        }

        if (!task.date) {
            //Pick a date code
            this.toast("Date chosen");
        }
        if (!task.time) {
            //Pick a time code
            this.toast("Time chosen");
        }

        try {
            const saved = await saveTaskToBackend(task);
            task.id = saved.id;   // use the database id from now on
        } catch (e) {
            this.toast("Could not save task");
            return;
        }

        this.close();
        if (this.onSaved) this.onSaved(task);
    }


};