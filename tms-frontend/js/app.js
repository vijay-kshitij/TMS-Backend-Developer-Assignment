// API Base URL - Change this if your backend runs on different port
const API_BASE_URL = 'http://localhost:8080/api';

// ============================================
// UTILITY FUNCTIONS
// ============================================

function showSection(sectionName) {
    // Hide all sections
    document.querySelectorAll('.content-section').forEach(section => {
        section.style.display = 'none';
    });

    // Remove active class from all nav links
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });

    // Show selected section
    document.getElementById(sectionName + 'Section').style.display = 'block';

    // Add active class to clicked nav link
    event.target.classList.add('active');

    // Load data for the section
    switch(sectionName) {
        case 'loads':
            fetchLoads();
            break;
        case 'transporters':
            fetchTransporters();
            break;
        case 'bids':
            fetchBids();
            break;
        case 'bookings':
            fetchBookings();
            break;
    }
}

function showToast(message, type = 'success') {
    const alertClass = type === 'success' ? 'alert-success' : 'alert-danger';
    const icon = type === 'success' ? 'check-circle' : 'exclamation-triangle';

    const alertHTML = `
        <div class="alert ${alertClass} alert-dismissible fade show" role="alert">
            <i class="bi bi-${icon}"></i> ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;

    // Insert at top of current section
    const currentSection = document.querySelector('.content-section:not([style*="display: none"])');
    currentSection.insertAdjacentHTML('afterbegin', alertHTML);

    // Auto-dismiss after 5 seconds
    setTimeout(() => {
        const alert = currentSection.querySelector('.alert');
        if (alert) alert.remove();
    }, 5000);
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
}

function getStatusBadgeClass(status) {
    const statusMap = {
        'POSTED': 'badge-posted',
        'OPEN_FOR_BIDS': 'badge-open',
        'BOOKED': 'badge-booked',
        'CANCELLED': 'badge-cancelled',
        'PENDING': 'badge-pending',
        'ACCEPTED': 'badge-accepted',
        'REJECTED': 'badge-rejected',
        'CONFIRMED': 'badge-confirmed'
    };
    return statusMap[status] || 'bg-secondary';
}

function clearFilters() {
    document.getElementById('loadStatusFilter').value = '';
    document.getElementById('shipperIdFilter').value = '';
    fetchLoads();
}

// ============================================
// LOADS SECTION
// ============================================

async function fetchLoads() {
    const listContainer = document.getElementById('loadsList');
    listContainer.innerHTML = '<div class="loading"><div class="spinner-border text-primary"></div><p class="mt-2">Loading loads...</p></div>';

    try {
        const status = document.getElementById('loadStatusFilter').value;
        const shipperId = document.getElementById('shipperIdFilter').value;

        let url = `${API_BASE_URL}/loads?page=0&size=100`;
        if (status) url += `&status=${status}`;
        if (shipperId) url += `&shipperId=${shipperId}`;

        const response = await fetch(url);

        if (!response.ok) {
            throw new Error('Failed to fetch loads');
        }

        const data = await response.json();
        const loads = data.content || [];

        if (loads.length === 0) {
            listContainer.innerHTML = `
                <div class="empty-state">
                    <i class="bi bi-inbox"></i>
                    <h4>No loads found</h4>
                    <p>Create a new load to get started</p>
                </div>
            `;
            return;
        }

        listContainer.innerHTML = loads.map(load => `
            <div class="card load-card mb-3">
                <div class="card-header d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-box-seam"></i> ${load.loadingCity} → ${load.unloadingCity}</span>
                    <span class="badge ${getStatusBadgeClass(load.status)}">${load.status.replace('_', ' ')}</span>
                </div>
                <div class="card-body">
                    <div class="row">
                        <div class="col-md-6">
                            <div class="info-row">
                                <span class="info-label">Load ID:</span>
                                <span class="info-value"><small>${load.loadId}</small></span>
                                <button class="btn btn-sm btn-outline-secondary" onclick="copyToClipboard('${load.loadId}')">
                                    <i class="bi bi-clipboard"></i>
                                </button>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Shipper ID:</span>
                                <span class="info-value">${load.shipperId}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Product:</span>
                                <span class="info-value">${load.productType}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Weight:</span>
                                <span class="info-value">${load.weight} ${load.weightUnit}</span>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-row">
                                <span class="info-label">Truck Type:</span>
                                <span class="info-value">${load.truckType}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Trucks Needed:</span>
                                <span class="info-value">${load.noOfTrucks}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Trucks Remaining:</span>
                                <span class="info-value"><strong>${load.remainingTrucks}</strong></span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Loading Date:</span>
                                <span class="info-value">${formatDate(load.loadingDate)}</span>
                            </div>
                        </div>
                    </div>
                    <div class="action-buttons mt-3">
                        ${load.status !== 'CANCELLED' && load.status !== 'BOOKED' ?
                            `<button class="btn btn-sm btn-danger" onclick="cancelLoad('${load.loadId}')">
                                <i class="bi bi-x-circle"></i> Cancel Load
                            </button>` : ''}
                        <button class="btn btn-sm btn-info" onclick="viewBestBids('${load.loadId}')">
                            <i class="bi bi-star"></i> Best Bids
                        </button>
                    </div>
                </div>
            </div>
        `).join('');

    } catch (error) {
        console.error('Error fetching loads:', error);
        listContainer.innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i>
                Failed to load data. Make sure the backend is running on ${API_BASE_URL}
            </div>
        `;
    }
}

async function createLoad() {
    const loadData = {
        shipperId: document.getElementById('shipperId').value,
        loadingCity: document.getElementById('loadingCity').value,
        unloadingCity: document.getElementById('unloadingCity').value,
        loadingDate: document.getElementById('loadingDate').value,
        productType: document.getElementById('productType').value,
        weight: parseFloat(document.getElementById('weight').value),
        weightUnit: document.getElementById('weightUnit').value,
        truckType: document.getElementById('truckType').value,
        noOfTrucks: parseInt(document.getElementById('noOfTrucks').value)
    };

    try {
        const response = await fetch(`${API_BASE_URL}/loads`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loadData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create load');
        }

        // Close modal
        const modal = bootstrap.Modal.getInstance(document.getElementById('createLoadModal'));
        modal.hide();

        // Reset form
        document.getElementById('createLoadForm').reset();

        // Refresh loads list
        fetchLoads();

        showToast('Load created successfully!', 'success');

    } catch (error) {
        console.error('Error creating load:', error);
        showToast(error.message, 'error');
    }
}

async function cancelLoad(loadId) {
    if (!confirm('Are you sure you want to cancel this load?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/loads/${loadId}/cancel`, {
            method: 'PATCH'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to cancel load');
        }

        fetchLoads();
        showToast('Load cancelled successfully!', 'success');

    } catch (error) {
        console.error('Error cancelling load:', error);
        showToast(error.message, 'error');
    }
}

async function viewBestBids(loadId) {
    try {
        const response = await fetch(`${API_BASE_URL}/loads/${loadId}/best-bids`);

        if (!response.ok) {
            throw new Error('Failed to fetch best bids');
        }

        const bids = await response.json();

        if (bids.length === 0) {
            alert('No bids available for this load yet.');
            return;
        }

        let bidsHTML = '<h5>Best Bids (Ranked by Score)</h5><div class="list-group">';
        bids.forEach((bid, index) => {
            bidsHTML += `
                <div class="list-group-item">
                    <div class="d-flex justify-content-between align-items-center">
                        <div>
                            <h6 class="mb-1">#${index + 1} - Transporter: ${bid.transporterId}</h6>
                            <p class="mb-1">Proposed Rate: ₹${bid.proposedRate} | Rating: ${bid.transporterRating}/5</p>
                            <small>Score: ${bid.score.toFixed(4)}</small>
                        </div>
                    </div>
                </div>
            `;
        });
        bidsHTML += '</div>';

        // Show in alert (you can also create a modal for this)
        const alertDiv = document.createElement('div');
        alertDiv.innerHTML = `
            <div class="modal fade" id="bestBidsModal" tabindex="-1">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title">Best Bids</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                        </div>
                        <div class="modal-body">
                            ${bidsHTML}
                        </div>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(alertDiv);
        const modal = new bootstrap.Modal(document.getElementById('bestBidsModal'));
        modal.show();

    } catch (error) {
        console.error('Error fetching best bids:', error);
        alert('Failed to load best bids');
    }
}

function copyToClipboard(text) {
    navigator.clipboard.writeText(text).then(() => {
        showToast('ID copied to clipboard!', 'success');
    });
}

// ============================================
// TRANSPORTERS SECTION
// ============================================

async function fetchTransporters() {
    const listContainer = document.getElementById('transportersList');
    listContainer.innerHTML = '<div class="loading"><div class="spinner-border text-primary"></div><p class="mt-2">Loading transporters...</p></div>';

    try {
        const response = await fetch(`${API_BASE_URL}/transporters`);

        if (!response.ok) {
            throw new Error('Failed to fetch transporters');
        }

        const transporters = await response.json();

        if (transporters.length === 0) {
            listContainer.innerHTML = `
                <div class="empty-state">
                    <i class="bi bi-truck"></i>
                    <h4>No transporters found</h4>
                    <p>Register a new transporter to get started</p>
                </div>
            `;
            return;
        }

        listContainer.innerHTML = transporters.map(transporter => `
            <div class="card transporter-card mb-3">
                <div class="card-body">
                    <h5 class="card-title">
                        <i class="bi bi-truck"></i> ${transporter.companyName}
                        <span class="badge bg-warning text-dark ms-2">
                            <i class="bi bi-star-fill"></i> ${transporter.rating}
                        </span>
                    </h5>
                    <div class="info-row">
                        <span class="info-label">Transporter ID:</span>
                        <span class="info-value"><small>${transporter.transporterId}</small></span>
                        <button class="btn btn-sm btn-outline-secondary" onclick="copyToClipboard('${transporter.transporterId}')">
                            <i class="bi bi-clipboard"></i>
                        </button>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Available Trucks:</span>
                    </div>
                    ${transporter.availableTrucks && transporter.availableTrucks.length > 0 ?
                        transporter.availableTrucks.map(truck => `
                            <div class="ms-4">
                                <span class="badge bg-secondary">${truck.truckType}</span>: ${truck.count} trucks
                            </div>
                        `).join('') :
                        '<div class="ms-4 text-muted">No trucks available</div>'}
                </div>
            </div>
        `).join('');

    } catch (error) {
        console.error('Error fetching transporters:', error);
        listContainer.innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> Failed to load transporters
            </div>
        `;
    }
}

function addTruckEntry() {
    const container = document.getElementById('trucksContainer');
    const entryCount = container.children.length;

    const newEntry = document.createElement('div');
    newEntry.className = 'truck-entry mb-3';
    newEntry.innerHTML = `
        <div class="row">
            <div class="col-md-6">
                <label class="form-label">Truck Type *</label>
                <select class="form-select truck-type-select" required>
                    <option value="">Select truck type</option>
                    <option value="Container">Container</option>
                    <option value="Flatbed">Flatbed</option>
                    <option value="Refrigerated">Refrigerated</option>
                    <option value="Tanker">Tanker</option>
                </select>
            </div>
            <div class="col-md-5">
                <label class="form-label">Number of Trucks *</label>
                <input type="number" class="form-control truck-count-input" min="1" required>
            </div>
            <div class="col-md-1 d-flex align-items-end">
                <button type="button" class="btn btn-danger btn-sm" onclick="removeTruckEntry(this)">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    `;

    container.appendChild(newEntry);

    // Show delete button on all entries when there's more than one
    updateDeleteButtons();
}

function removeTruckEntry(button) {
    const entry = button.closest('.truck-entry');
    entry.remove();
    updateDeleteButtons();
}

function updateDeleteButtons() {
    const entries = document.querySelectorAll('.truck-entry');
    entries.forEach((entry, index) => {
        const deleteBtn = entry.querySelector('.btn-danger');
        if (entries.length > 1) {
            deleteBtn.style.display = 'block';
        } else {
            deleteBtn.style.display = 'none';
        }
    });
}

async function registerTransporter() {
    // Collect all truck entries
    const truckEntries = document.querySelectorAll('.truck-entry');
    const availableTrucks = [];

    // Validate and collect truck data
    let isValid = true;
    const usedTypes = new Set();

    truckEntries.forEach(entry => {
        const truckType = entry.querySelector('.truck-type-select').value;
        const count = entry.querySelector('.truck-count-input').value;

        if (!truckType || !count) {
            isValid = false;
            return;
        }

        // Check for duplicate truck types
        if (usedTypes.has(truckType)) {
            alert(`Duplicate truck type: ${truckType}. Please use different truck types or combine them.`);
            isValid = false;
            return;
        }

        usedTypes.add(truckType);
        availableTrucks.push({
            truckType: truckType,
            count: parseInt(count)
        });
    });

    if (!isValid) {
        return;
    }

    const transporterData = {
        companyName: document.getElementById('companyName').value,
        rating: parseFloat(document.getElementById('rating').value),
        availableTrucks: availableTrucks
    };

    try {
        const response = await fetch(`${API_BASE_URL}/transporters`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(transporterData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to register transporter');
        }

        const modal = bootstrap.Modal.getInstance(document.getElementById('registerTransporterModal'));
        modal.hide();

        // Reset form and truck entries
        document.getElementById('registerTransporterForm').reset();
        const container = document.getElementById('trucksContainer');
        container.innerHTML = `
            <div class="truck-entry mb-3">
                <div class="row">
                    <div class="col-md-6">
                        <label class="form-label">Truck Type *</label>
                        <select class="form-select truck-type-select" required>
                            <option value="">Select truck type</option>
                            <option value="Container">Container</option>
                            <option value="Flatbed">Flatbed</option>
                            <option value="Refrigerated">Refrigerated</option>
                            <option value="Tanker">Tanker</option>
                        </select>
                    </div>
                    <div class="col-md-5">
                        <label class="form-label">Number of Trucks *</label>
                        <input type="number" class="form-control truck-count-input" min="1" required>
                    </div>
                    <div class="col-md-1 d-flex align-items-end">
                        <button type="button" class="btn btn-danger btn-sm" onclick="removeTruckEntry(this)" style="display: none;">
                            <i class="bi bi-trash"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;

        fetchTransporters();

        showToast('Transporter registered successfully!', 'success');

    } catch (error) {
        console.error('Error registering transporter:', error);
        showToast(error.message, 'error');
    }
}


// ============================================
// BIDS SECTION
// ============================================

async function fetchBids() {
    const listContainer = document.getElementById('bidsList');
    listContainer.innerHTML = '<div class="loading"><div class="spinner-border text-primary"></div><p class="mt-2">Loading bids...</p></div>';

    try {
        const response = await fetch(`${API_BASE_URL}/bids`);

        if (!response.ok) {
            throw new Error('Failed to fetch bids');
        }

        const bids = await response.json();

        if (bids.length === 0) {
            listContainer.innerHTML = `
                <div class="empty-state">
                    <i class="bi bi-cash-stack"></i>
                    <h4>No bids found</h4>
                    <p>Submit a bid to get started</p>
                </div>
            `;
            return;
        }

        listContainer.innerHTML = bids.map(bid => `
            <div class="card bid-card mb-3">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h6><i class="bi bi-cash-stack"></i> Bid #${bid.bidId.substring(0, 8)}...</h6>
                        <span class="badge ${getStatusBadgeClass(bid.status)}">${bid.status}</span>
                    </div>
                    <div class="row">
                        <div class="col-md-6">
                            <div class="info-row">
                                <span class="info-label">Load ID:</span>
                                <span class="info-value"><small>${bid.loadId}</small></span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Transporter ID:</span>
                                <span class="info-value"><small>${bid.transporterId}</small></span>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-row">
                                <span class="info-label">Proposed Rate:</span>
                                <span class="info-value">₹${bid.proposedRate}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Trucks Offered:</span>
                                <span class="info-value">${bid.trucksOffered}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Submitted:</span>
                                <span class="info-value">${formatDate(bid.submittedAt)}</span>
                            </div>
                        </div>
                    </div>
                    ${bid.status === 'PENDING' ? `
                        <div class="action-buttons mt-3">
                            <button class="btn btn-sm btn-success" onclick="acceptBid('${bid.bidId}', ${bid.trucksOffered}, ${bid.proposedRate})">
                                <i class="bi bi-check-circle"></i> Accept Bid
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="rejectBid('${bid.bidId}')">
                                <i class="bi bi-x-circle"></i> Reject Bid
                            </button>
                        </div>
                    ` : ''}
                </div>
            </div>
        `).join('');

    } catch (error) {
        console.error('Error fetching bids:', error);
        listContainer.innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> Failed to load bids
            </div>
        `;
    }
}

async function submitBid() {
    const bidData = {
        loadId: document.getElementById('bidLoadId').value,
        transporterId: document.getElementById('bidTransporterId').value,
        proposedRate: parseFloat(document.getElementById('proposedRate').value),
        trucksOffered: parseInt(document.getElementById('trucksOffered').value)
    };

    try {
        const response = await fetch(`${API_BASE_URL}/bids`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(bidData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to submit bid');
        }

        const modal = bootstrap.Modal.getInstance(document.getElementById('submitBidModal'));
        modal.hide();

        document.getElementById('submitBidForm').reset();

        fetchBids();

        showToast('Bid submitted successfully!', 'success');

    } catch (error) {
        console.error('Error submitting bid:', error);
        showToast(error.message, 'error');
    }
}

async function rejectBid(bidId) {
    if (!confirm('Are you sure you want to reject this bid?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/bids/${bidId}/reject`, {
            method: 'PATCH'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to reject bid');
        }

        fetchBids();
        showToast('Bid rejected successfully!', 'success');

    } catch (error) {
        console.error('Error rejecting bid:', error);
        showToast(error.message, 'error');
    }
}

async function acceptBid(bidId, allocatedTrucks, finalRate) {
    if (!confirm('Accept this bid and create booking?')) {
        return;
    }

    const bookingData = {
        allocatedTrucks: allocatedTrucks,
        finalRate: finalRate
    };

    try {
        const response = await fetch(`${API_BASE_URL}/bookings?bidId=${bidId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(bookingData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to create booking');
        }

        fetchBids();
        showToast('Bid accepted! Booking created successfully!', 'success');

    } catch (error) {
        console.error('Error accepting bid:', error);
        showToast(error.message, 'error');
    }
}

// ============================================
// BOOKINGS SECTION
// ============================================

async function fetchBookings() {
    const listContainer = document.getElementById('bookingsList');
    listContainer.innerHTML = '<div class="loading"><div class="spinner-border text-primary"></div><p class="mt-2">Loading bookings...</p></div>';

    try {
        const response = await fetch(`${API_BASE_URL}/bookings`);

        if (!response.ok) {
            throw new Error('Failed to fetch bookings');
        }

        const bookings = await response.json();

        if (bookings.length === 0) {
            listContainer.innerHTML = `
                <div class="empty-state">
                    <i class="bi bi-check-circle"></i>
                    <h4>No bookings found</h4>
                    <p>Accept a bid to create a booking</p>
                </div>
            `;
            return;
        }

        listContainer.innerHTML = bookings.map(booking => `
            <div class="card booking-card mb-3">
                <div class="card-body">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <h6><i class="bi bi-check-circle"></i> Booking #${booking.bookingId.substring(0, 8)}...</h6>
                        <span class="badge ${getStatusBadgeClass(booking.status)}">${booking.status}</span>
                    </div>
                    <div class="row">
                        <div class="col-md-6">
                            <div class="info-row">
                                <span class="info-label">Booking ID:</span>
                                <span class="info-value"><small>${booking.bookingId}</small></span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Load ID:</span>
                                <span class="info-value"><small>${booking.loadId}</small></span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Bid ID:</span>
                                <span class="info-value"><small>${booking.bidId}</small></span>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="info-row">
                                <span class="info-label">Transporter ID:</span>
                                <span class="info-value"><small>${booking.transporterId}</small></span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Allocated Trucks:</span>
                                <span class="info-value">${booking.allocatedTrucks}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Final Rate:</span>
                                <span class="info-value">₹${booking.finalRate}</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">Booked At:</span>
                                <span class="info-value">${formatDate(booking.bookedAt)}</span>
                            </div>
                        </div>
                    </div>
                    ${booking.status === 'CONFIRMED' ? `
                        <div class="action-buttons mt-3">
                            <button class="btn btn-sm btn-danger" onclick="cancelBooking('${booking.bookingId}')">
                                <i class="bi bi-x-circle"></i> Cancel Booking
                            </button>
                        </div>
                    ` : ''}
                </div>
            </div>
        `).join('');

    } catch (error) {
        console.error('Error fetching bookings:', error);
        listContainer.innerHTML = `
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> Failed to load bookings
            </div>
        `;
    }
}

async function cancelBooking(bookingId) {
    if (!confirm('Are you sure you want to cancel this booking?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/bookings/${bookingId}/cancel`, {
            method: 'PATCH'
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to cancel booking');
        }

        fetchBookings();
        showToast('Booking cancelled successfully!', 'success');

    } catch (error) {
        console.error('Error cancelling booking:', error);
        showToast(error.message, 'error');
    }
}

// ============================================
// INITIALIZE ON PAGE LOAD
// ============================================

document.addEventListener('DOMContentLoaded', function() {
    // Load initial data
    fetchLoads();

    console.log('TMS Frontend initialized!');
    console.log('Backend URL:', API_BASE_URL);
});