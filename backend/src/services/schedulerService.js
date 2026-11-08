const taskService = require('./taskService');

async function organiseDateTasks(userId, date) {
    const tasks = await taskService.getTasksByUserAndDate(userId, date);
    return tasks;
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
    const results = [];
    for (const day of days) {
        results.push(await scheduleDay(userId, day)); // reuse the day logic
    }
    return results;
}



module.exports = {
    organiseDateTasks, scheduleWeek
};