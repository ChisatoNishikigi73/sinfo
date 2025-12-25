// API Documentation Generator
let apiData = null;

// Initialize the app
async function init() {
    try {
        const response = await fetch('api-data.json');
        apiData = await response.json();
        renderPage();
    } catch (error) {
        console.error('Failed to load API data:', error);
        document.body.innerHTML = '<div class="loading">Failed to load API documentation</div>';
    }
}

// Render the entire page
function renderPage() {
    renderHeader();
    renderIntroduction();
    renderAuthentication();
    renderEndpoints();
    renderStatusCodes();
    renderConfiguration();
}

// Render header
function renderHeader() {
    const headerContainer = document.getElementById('header-container');
    headerContainer.innerHTML = `
        <h1>${apiData.title}</h1>
        <div class="base-url">Base URL: ${apiData.baseUrl}</div>
    `;
}

// Render introduction section
function renderIntroduction() {
    const container = document.getElementById('introduction');
    const features = apiData.introduction.features
        .map(f => `<li>${f}</li>`)
        .join('');
    
    container.innerHTML = `
        <h2>Introduction</h2>
        <p>${apiData.introduction.description}</p>
        <p><strong>Features:</strong></p>
        <ul>${features}</ul>
    `;
}

// Render authentication section
function renderAuthentication() {
    const container = document.getElementById('authentication');
    
    const responseFormats = apiData.responseFormat.map(format => `
        <p><strong>${format.type}:</strong></p>
        <div class="code-block">${JSON.stringify(format.example, null, 2)}</div>
    `).join('');
    
    container.innerHTML = `
        <h2>Authentication</h2>
        <p>${apiData.authentication.description}</p>
        <div class="code-block">${apiData.authentication.header}</div>
        <div class="auth-info">
            <strong>Note:</strong> ${apiData.authentication.note}
        </div>
        <p><strong>Response Format:</strong></p>
        ${responseFormats}
    `;
}

// Render all API endpoints
function renderEndpoints() {
    const container = document.getElementById('endpoints');
    let html = '<h2>API Endpoints</h2>';
    
    apiData.categories.forEach(category => {
        html += renderCategory(category);
    });
    
    container.innerHTML = html;
}

// Render a single category
function renderCategory(category) {
    const endpoints = category.endpoints.map(endpoint => renderEndpoint(endpoint)).join('');
    return `
        <div class="category">
            <h3>${category.name}</h3>
            <div class="api-list">
                ${endpoints}
            </div>
        </div>
    `;
}

// Render a single endpoint
function renderEndpoint(endpoint) {
    const authBadge = endpoint.requiresAuth
        ? '<span class="badge badge-auth">Auth Required</span>'
        : '<span class="badge badge-no-auth">No Auth</span>';
    
    const methodClass = endpoint.method.toLowerCase() === 'get' ? 'method-get' : 'method-post';
    
    return `
        <div class="api-item">
            <div class="api-header" onclick="toggleApi(this)">
                <span class="api-method ${methodClass}">${endpoint.method}</span>
                <span class="api-path">${endpoint.path}</span>
                <div class="api-badge">${authBadge}</div>
                <span class="expand-icon">▼</span>
            </div>
            <div class="api-content">
                <div class="api-details">
                    ${renderEndpointDetails(endpoint)}
                </div>
            </div>
        </div>
    `;
}

// Render endpoint details
function renderEndpointDetails(endpoint) {
    let html = `
        <div class="detail-section">
            <h4>Description</h4>
            <p>${endpoint.description}</p>
        </div>
    `;
    
    // Query parameters
    if (endpoint.queryParams && endpoint.queryParams.length > 0) {
        html += `
            <div class="detail-section">
                <h4>Query Parameters</h4>
                ${renderParamTable(endpoint.queryParams)}
            </div>
        `;
    }
    
    // Body parameters
    if (endpoint.bodyParams && endpoint.bodyParams.length > 0) {
        html += `
            <div class="detail-section">
                <h4>Request Body</h4>
                ${renderParamTable(endpoint.bodyParams)}
            </div>
        `;
    }
    
    // Note
    if (endpoint.note) {
        html += `
            <div class="note">
                <strong>Note:</strong> ${endpoint.note}
            </div>
        `;
    }
    
    // Warning
    if (endpoint.warning) {
        html += `
            <div class="warning">
                <strong>Warning:</strong> ${endpoint.warning}
            </div>
        `;
    }
    
    // Request example
    if (endpoint.requestExample) {
        html += `
            <div class="detail-section">
                <h4>Request Example</h4>
                <div class="code-block">${JSON.stringify(endpoint.requestExample, null, 2)}</div>
            </div>
        `;
    }
    
    // Event types (for event stream endpoint)
    if (endpoint.eventTypes) {
        html += `
            <div class="detail-section">
                <h4>Event Types</h4>
                <ul>
                    ${endpoint.eventTypes.map(e => `<li><code>${e.name}</code> - ${e.description}</li>`).join('')}
                </ul>
            </div>
        `;
    }
    
    // Event examples
    if (endpoint.eventExamples) {
        html += `
            <div class="detail-section">
                <h4>Event Format Examples</h4>
                <div class="code-block">${endpoint.eventExamples.map(e => `event: ${e.event}\ndata: ${e.data}`).join('\n\n')}</div>
            </div>
        `;
    }
    
    // Response example
    if (endpoint.responseExample) {
        html += `
            <div class="detail-section">
                <h4>Response Example</h4>
                <div class="code-block">${JSON.stringify(endpoint.responseExample, null, 2)}</div>
            </div>
        `;
    }
    
    // Tip
    if (endpoint.tip) {
        html += `
            <div class="note">
                <strong>Tip:</strong> ${endpoint.tip}
            </div>
        `;
    }
    
    return html;
}

// Render parameter table
function renderParamTable(params) {
    const rows = params.map(param => {
        const requiredBadge = param.required
            ? '<span class="param-required">Required</span>'
            : '<span class="param-optional">Optional</span>';
        
        return `
            <tr>
                <td><code>${param.name}</code></td>
                <td>${param.type}</td>
                <td>${requiredBadge}</td>
                <td>${param.description}</td>
            </tr>
        `;
    }).join('');
    
    return `
        <table class="param-table">
            <thead>
                <tr>
                    <th>Parameter</th>
                    <th>Type</th>
                    <th>Required</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                ${rows}
            </tbody>
        </table>
    `;
}

// Render status codes
function renderStatusCodes() {
    const container = document.getElementById('status-codes');
    const rows = apiData.statusCodes.map(status => `
        <tr>
            <td><code>${status.code}</code></td>
            <td>${status.description}</td>
        </tr>
    `).join('');
    
    container.innerHTML = `
        <h2>HTTP Status Codes</h2>
        <table class="param-table">
            <thead>
                <tr>
                    <th>Code</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                ${rows}
            </tbody>
        </table>
    `;
}

// Render configuration
function renderConfiguration() {
    const container = document.getElementById('configuration');
    const fields = apiData.configuration.fields.map(field => `
        <tr>
            <td><code>${field.name}</code></td>
            <td>${field.type}</td>
            <td><code>${JSON.stringify(field.default)}</code></td>
            <td>${field.description}</td>
        </tr>
    `).join('');
    
    container.innerHTML = `
        <h2>Configuration</h2>
        <p><strong>Configuration File:</strong> <code>${apiData.configuration.file}</code></p>
        <table class="param-table">
            <thead>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Default</th>
                    <th>Description</th>
                </tr>
            </thead>
            <tbody>
                ${fields}
            </tbody>
        </table>
        <div class="note" style="margin-top: 15px;">
            <strong>Note:</strong> ${apiData.configuration.note}
        </div>
    `;
}

// Toggle API item
function toggleApi(header) {
    const apiItem = header.parentElement;
    const wasActive = apiItem.classList.contains('active');
    
    // Close all other items
    document.querySelectorAll('.api-item').forEach(item => {
        item.classList.remove('active');
    });
    
    // Toggle current item
    if (!wasActive) {
        apiItem.classList.add('active');
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', init);

