document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('login-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        const response = await fetch('/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ email, password })
        });

        const data = await response.json();
        const messageDiv = document.getElementById('message');

        if (response.ok) {
            messageDiv.style.color = 'green';
            messageDiv.textContent = 'Login successful!';
            setTimeout(() => {
                window.location.href = '/admin_home';
            }, 1000);
        } else {
            messageDiv.style.color = 'red';
            messageDiv.textContent = data.error;
        }
    });
});
