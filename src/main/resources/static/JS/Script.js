function confirmDelete(){
    return (confirm("Выдествительно хотите удалить?"))
}

function confirmAdd() {
    console.log('confirmAdd called');
    return (confirm('Вы действительно хотите добавить?'));
}
function updateSkills() {
    const categorySelect = document.getElementById('category');
    const skillSelect = document.getElementById('skill');
    const selectedCategory = categorySelect.value;

    skillSelect.innerHTML = '<option value="">Все навыки</option>';

    const skillsByCategory = {
        'Backend': ['JAVA', 'SPRING', 'SPRING_BOOT', 'HIBERNATE', 'JPA', 'SQL'],
        'Frontend': ['HTML', 'CSS', 'JAVASCRIPT', 'REACT', 'ANGULAR', 'VUE'],
        'DevOps': ['DOCKER', 'HIBERNET', 'AWS', 'AZURE'],
        'Tools': ['GIT', 'MAVEN', 'GRADLE', 'POSTMAN'],
        'Testing': ['JUNIT', 'MOCKITO', 'TESTING']
    };
    if (selectedCategory && skillsByCategory[selectedCategory]) {
        skillsByCategory[selectedCategory].forEach(skill => {
            const option = document.createElement('option');
            option.value = skill;
            option.textContent = skill;
            skillSelect.appendChild(option);
        });
    }
}
