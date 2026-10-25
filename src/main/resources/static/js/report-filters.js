(() => {
    const departmentFilter = document.querySelector("#departmentFilter");
    const projectFilter = document.querySelector("#projectFilter");
    const help = document.querySelector("#projectFilterHelp");

    if (!departmentFilter || !projectFilter || !help) return;

    const allProjectsOption = projectFilter.options[0];
    const projectOptions = Array.from(projectFilter.options).slice(1);

    const updateProjects = () => {
        const departmentId = departmentFilter.value;
        let visibleProjects = 0;

        projectOptions.forEach((option) => {
            const visible = !departmentId || option.dataset.departmentId === departmentId;
            option.hidden = !visible;
            if (visible) visibleProjects += 1;
            if (!visible && option.selected) projectFilter.value = "";
        });

        allProjectsOption.textContent = departmentId
            ? "選択した部門のすべてのプロジェクト"
            : "すべてのプロジェクト";
        help.textContent = departmentId && visibleProjects === 0
            ? "この部門には選択可能なプロジェクトがありません。"
            : "部門を選ぶと、その部門のプロジェクトに絞られます。";
    };

    departmentFilter.addEventListener("change", updateProjects);
    updateProjects();
})();
