const taskService = require('./taskService');

async function moveTaskForward(task, date) {
    // MOVE TASK DATE
}

async function scheduleDay(userId, date) {
    const allTasks = await taskService.getTasksByUserAndDate(userId, date);
    const orderedTasks = allTasks.sort((a,b) => a.priority - b.priority); // 1 = highest priority so ascending sort
    let totalTime = orderedTasks.reduce((total,task) => total+task.duration,0);
    // If day over 8 hours of work attempt to move a movable task
    const maxTime = 480;
    const maxAttempts = 5;
    let attempts = 1;
    while(totalTime > maxTime && attempts<maxAttempts){
        const endOfDate = new Date(date);
        endOfDate.setHours(23, 59, 59, 0);
        const reversedOrderedTasks = orderedTasks.toReversed(); // Sorted by lowest priority first
        // Task with a deadline after the end of this date that is not fixed to this date
        const movableTask = reversedOrderedTasks.find(task => (task.deadline > endOfDate || task.deadline===""|| task.deadline===null ) && !task.dateIsFixed );
        if(movableTask){
            await moveTaskForward(movableTask, date);
            const index = orderedTasks.indexOf(movableTask);
            orderedTasks.splice(index, 1);
        }
        else{
            if(totalTime>840){
                // Give up if more than 14 hours of tasks and send failer back
                return {
                    success: false,
                    error: new Error("Too many tasks for one day")
                };
            }
            else{
                continue;
            }
        }

        totalTime = orderedTasks.reduce((total,task) => total+task.duration,0);
        attempts +=1;
    }
    if(attempts>maxAttempts){
        return {
            success: false,
            error: new Error("Too many attempts")
        };
    }
    for (const task in orderedTasks){
        // SET TIMES FOR EACH TASK IN DB
    }
    return { success: true };
}


async function scheduleWeek(userId, startDate) {
    function getSevenDays(startDate) {
        return Array.from({ length: 7 }, (_, i) => {
            const date = new Date(startDate);
            date.setDate(startDate.getDate() + i);
            return date;
        });
    }

    const days = getSevenDays(startDate);
    for (const day of days) {
        const isSuccess = await scheduleDay(userId, day);
        if(!isSuccess.success){
            return isSuccess;
        }
    }
    return { success: true };
}



module.exports = {
    scheduleDay, scheduleWeek
};