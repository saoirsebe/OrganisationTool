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
    const result = await res.json();
    if (!result.success) {
        $("errorText").textContent = result.error;
        $("errorMessage").classList.add("visible");
        return;
    }
    if(!res.ok){
        throw new Error("Save failed");
    }
    return result;   // the saved task, including its database id
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

    updateFixedOptions() {
        const taskDate = $("date");
        const taskTime = $("time");
        const hasDate = taskDate.value !== '';
        const hasTime = taskTime.value !== '';

        // Show date checkbox only when a date exists
        $("dateFixedContainer").style.display = hasDate ? 'flex' : 'none';

        // Show time checkbox only when both date and time exist
        $("timeFixedContainer").style.display = hasDate && hasTime ? 'flex' : 'none';

        // Reset checkboxes if their inputs are removed
        const timeIsFixed = $('timeIsFixed');
        if (!hasDate) {
            const dateIsFixed = $('dateIsFixed');
            if(dateIsFixed.checked){
                dateIsFixed.checked = false;
                timeIsFixed.checked = false;
                timeIsFixed.dispatchEvent(new Event("change", { bubbles: true }));
            }
            if(hasTime){
                taskTime.value = '';
            }
        } else if (!hasTime) {
            if(timeIsFixed.checked){
                timeIsFixed.checked = false;
                timeIsFixed.dispatchEvent(new Event("change", { bubbles: true }));
            }
        }
    },

    // Hide deadline options when fixed date and time is chosen
    // Ensures timeIsFixed is not checked if dateIsFixed is not checked
    // Sets deadline date as task date if task date fixed
    updateDeadlineOptions() {
        const dateIsFixed = $('dateIsFixed');
        const timeIsFixed = $('timeIsFixed');
        const isTimeFixed = timeIsFixed.checked;

        if (isTimeFixed) {
            $("deadlineTitle").style.display = 'none';
            $("deadlineContainer").style.display = 'none';
        }
        else{
            $("deadlineTitle").style.display = 'flex';
            $("deadlineContainer").style.display = 'flex';
            if (dateIsFixed){
                $("deadlineDate").value = $("date").value;
            }
        }
    },

    init() {
        document.body.insertAdjacentHTML("beforeend", `
      <div class="overlay" id="overlay">
        <div class="popup" role="dialog" aria-modal="true" aria-labelledby="popupTitle">
          <h3 id="popupTitle">New Task</h3>
          
          <label for="desc">Description *</label>
          <textarea id="desc" placeholder="What needs doing?"></textarea>
          
          <div class="row">
            <label for="priority">Priority *</label>
            <select id="priority">
              <option value="1">High</option>
              <option value="2" selected>Medium</option>
              <option value="3">Low</option>
            </select>
          </div>
          
          <div class="row">
            <div><label for="date">Date (optional)</label><input type="date" id="date"></div>
            <div><label for="time">Time (optional)</label>
            <div class="time-input-wrapper">
                <input type="time" id="time">
                <button type="button" id="clearTimeBtn" aria-label="Clear time" title="Clear time">×</button>
                </div>
            </div>
          </div>
          <div id="dateError" style="display: none; color: red; font-size: 13px;">
            Please enter a date if you want to specify a time.
          </div>
          
          <div class="row fixed-options">
            <div id="dateFixedContainer" style="display: none;">
                <label for="dateIsFixed">Date is fixed</label>
                <input type="checkbox" id="dateIsFixed">
            </div>
            <div id="timeFixedContainer" style="display: none;">
                <label for="timeIsFixed">Time is fixed</label>
                <input type="checkbox" id="timeIsFixed">
            </div>
          </div>
          
          <label for="durHours">Duration (optional)</label>
          <div class="row">
            <div><input type="number" id="durHours" min="0" max="23" placeholder="Hours"></div>
            <div><input type="number" id="durMins" min="0" max="59" placeholder="Minutes"></div>
          </div>
          
 
          <label id ="deadlineTitle" for="deadlineDate">Task deadline (Optional)</label>
          <div id ="deadlineContainer" class="row">
            <label for="deadlineDate">Date</label>
            <div><input type="date" id="deadlineDate"></div>
            <label for="deadlineTime">Time</label>
            <div><input type="time" id="deadlineTime"></div>
          </div>
          <div id="deadlineDateError" style="display: none; color: red; font-size: 13px;">
            Please enter a deadline date if you want to specify a time.
          </div>
          <div id="errorMessage" class="error-message">
              <strong>Unable to schedule tasks</strong>
              <span id="errorText"></span>
          </div>
          
          <div class="actions">
            <button id="cancelBtn">Cancel</button>
            <button class="primary" id="saveBtn">Save</button>
          </div>
        </div>
      </div>
      <div id="toast"></div>`);
        // Save, close functionality
        $("cancelBtn").addEventListener("click", () => this.close());
        $("saveBtn").addEventListener("click", () => this.save());
        $("overlay").addEventListener("click", e => { if (e.target === $("overlay")) this.close(); });
        document.addEventListener("keydown", e => { if (e.key === "Escape") this.close(); });


        // Updates deadline options & isFixed values
        const dateIsFixed = $("dateIsFixed");
        const timeIsFixed = $("timeIsFixed");
        timeIsFixed.addEventListener('change', () => {
            if(timeIsFixed.checked && !dateIsFixed.checked){
                dateIsFixed.checked = true;
                // dateIsFixed.dispatchEvent(new Event("change", { bubbles: true })); // Not needed??
            }
            this.updateDeadlineOptions()
        });
        dateIsFixed.addEventListener('change', () => {
            if(timeIsFixed.checked && !dateIsFixed.checked){
                timeIsFixed.checked = false;
                // dateIsFixed.dispatchEvent(new Event("change", { bubbles: true })); // Not needed??
            }
            this.updateDeadlineOptions()
        });

        // dateIsFixed only shows if date set, timeIsFixed only shows if time set (using updateFixedOptions())
        // If time is set, option to clear time is shown with x button using updateClearTimeButton()
        const timeInput = $("time");
        const updateClearTimeButton = () => {
            $("clearTimeBtn").style.display =  timeInput.value ? "block" : "none";
        };
        $("date").addEventListener('input', () => {
            this.updateFixedOptions();
            const errorMessage = $("errorMessage");
            if (errorMessage.classList.contains("visible")) {
                errorMessage.classList.remove("visible");
            }
        });

        timeInput.addEventListener('input', () => {
            this.updateFixedOptions();
            updateClearTimeButton();
        });

        $("clearTimeBtn").addEventListener("click", () => {
            timeInput.value = "";
            timeInput.dispatchEvent(new Event("input", { bubbles: true })); // Triggers event listener
        });

        this.updateFixedOptions();
        updateClearTimeButton();
        this.updateDeadlineOptions();
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
        // Input validation:
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
        // Check if time is entered without a date
        const dateError = $("dateError");
        const date = $("date");
        const time = $("time");
        if (time.value !== "" && date.value === "") {
            dateError.style.display = "block";
            return;
        }
        dateError.style.display = "none";
        // Check if deadline time entered without a date
        const deadlineDateError = $("deadlineDateError");
        const deadlineDate = $("deadlineDate").value;
        const deadlineTime = $("deadlineTime").value;
        if (deadlineTime !== "" && deadlineDate === "") {
            deadlineDateError.style.display = "block";
            return;
        }
        deadlineDateError.style.display = "none";

        const hours = Math.max(parseInt($("durHours").value, 10) || 0, 0);
        const mins = Math.max(parseInt($("durMins").value, 10) || 0, 0);


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